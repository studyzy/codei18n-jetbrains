package com.github.studyzy.codei18n.services;

import com.github.studyzy.codei18n.models.CliResponse;
import com.github.studyzy.codei18n.models.TranslatedComment;
import com.github.studyzy.codei18n.settings.PluginSettings;
import com.github.studyzy.codei18n.utils.JsonParser;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.Alarm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service(Service.Level.PROJECT)
public final class TranslationService implements Disposable {
    private static final Logger LOG = Logger.getInstance(TranslationService.class);
    private final Project project;
    private final Map<String, List<TranslatedComment>> cache;
    private final Alarm debounceAlarm;

    public TranslationService(Project project) {
        this.project = project;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, List<TranslatedComment>>(100, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<TranslatedComment>> eldest) {
                return size() > 100;
            }
        });
        this.debounceAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    }

    public static TranslationService getInstance(Project project) {
        return project.getService(TranslationService.class);
    }

    @Override
    public void dispose() {
        // Alarm is disposed automatically
    }

    /**
     * 获取翻译数据
     * @param file 文件
     * @param forceRefresh 是否强制刷新
     * @return 翻译列表
     */
    public List<TranslatedComment> getTranslations(PsiFile file, boolean forceRefresh) {
        String relativePath = getRelativePath(file);

        if (!forceRefresh && cache.containsKey(relativePath)) {
            return cache.get(relativePath);
        }
        
        // 如果缓存中没有，触发后台刷新并返回空列表
        // FoldingBuilder 会在数据准备好后通过 updateFolding 被重新触发
        scheduleRefresh(file, relativePath);
        
        return cache.getOrDefault(relativePath, Collections.emptyList());
    }
    
    /**
     * 同步获取翻译数据（带超时），用于 FoldingBuilder
     * @param file 文件
     * @param timeoutMs 超时时间（毫秒）
     * @return 翻译列表
     */
    public List<TranslatedComment> getTranslationsSync(PsiFile file, long timeoutMs) {
        String relativePath = getRelativePath(file);
        
        // 如果缓存中有数据，直接返回
        if (cache.containsKey(relativePath)) {
            return cache.get(relativePath);
        }
        
        // 没有缓存，需要同步获取
        final String content = file.getText();
        
        try {
            // 在后台线程同步执行（避免在ReadAction内直接运行外部进程）
            List<TranslatedComment> result = fetchTranslationsSync(file, relativePath, content, timeoutMs);
            if (result != null && !result.isEmpty()) {
                cache.put(relativePath, result);
                return result;
            }
        } catch (Exception e) {
            LOG.warn("Failed to fetch translations synchronously", e);
        }
        
        // 如果同步获取失败，触发异步刷新
        scheduleRefresh(file, relativePath);
        return Collections.emptyList();
    }
    
    /**
     * 同步获取翻译数据（直接调用 CLI），在后台线程运行以避免 ReadAction/EDT 阻塞
     */
    private List<TranslatedComment> fetchTranslationsSync(PsiFile file, String relativePath, String content, long timeoutMs) {
        CliService cliService = CliService.getInstance(project);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> jsonRef = new AtomicReference<>();
        
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String json = cliService.scanFile(
                    relativePath,
                    true,
                    true,
                    content
                );
                jsonRef.set(json);
            } finally {
                latch.countDown();
            }
        });
        
        try {
            boolean done = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!done) {
                LOG.warn("CLI execution timeout for file: " + relativePath);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("CLI execution interrupted for file: " + relativePath, e);
            return null;
        }
        
        String json = jsonRef.get();
        if (json == null) {
            LOG.warn("CLI returned null for file: " + relativePath);
            return null;
        }
        
        // TEMPORARY: Force log CLI response to debug the issue
        LOG.warn("CLI response for " + relativePath + ": " + json);

        CliResponse response = JsonParser.parseCliResponse(json);
        if (response == null) {
            LOG.warn("Failed to parse CLI response for file: " + relativePath);
            return null;
        }
        if (response.errors() != null && !response.errors().isEmpty()) {
            LOG.warn("CLI reported errors for file: " + relativePath + " -> " + response.errors());
        }
        List<CliResponse.CommentData> commentDataList = response.comments();
        if (commentDataList == null) {
            LOG.info("CLI returned no comments for file: " + relativePath);
            return Collections.emptyList();
        }

        List<TranslatedComment> translations = new ArrayList<>();
        
        // Document access needs ReadAction
        Document document = ReadAction.compute(() -> 
            PsiDocumentManager.getInstance(project).getDocument(file)
        );
        
        if (document != null) {
            for (CliResponse.CommentData data : response.comments()) {
                if (data.getTranslation() != null && !data.getTranslation().isEmpty()) {
                    int startLine = data.range().startLine() - 1;
                    int startCol = data.range().startCol() - 1;
                    int endLine = data.range().endLine() - 1;
                    int endCol = data.range().endCol() - 1;
                    
                    if (startLine >= 0 && startLine < document.getLineCount() && 
                        endLine >= 0 && endLine < document.getLineCount()) {
                        int startOffset = document.getLineStartOffset(startLine) + startCol;
                        int endOffset = document.getLineStartOffset(endLine) + endCol;
                        
                        TranslatedComment comment = new TranslatedComment(
                            data.id(),
                            data.sourceText(),
                            data.getTranslation(),
                            startOffset,
                            endOffset,
                            data.range().startLine(),
                            TranslatedComment.CommentType.LINE,
                            data.symbol()
                        );
                        translations.add(comment);
                    }
                }
            }
        }
        
        LOG.info("Fetched " + translations.size() + " translations for file: " + relativePath);
        return translations;
    }

    private void scheduleRefresh(PsiFile file, String relativePath) {
        debounceAlarm.cancelAllRequests();
        
        // Capture content in current thread (likely ReadAction or EDT)
        final String content = file.getText();
        
        debounceAlarm.addRequest(() -> {
            List<TranslatedComment> translations = fetchTranslationsSync(file, relativePath, content, 5000);
            
            if (translations != null) {
                cache.put(relativePath, translations);
                
                // Refresh UI on EDT
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!project.isDisposed() && file.isValid()) {
                        // 触发代码分析器重新分析
                        DaemonCodeAnalyzer.getInstance(project).restart(file);
                        
                        // 触发代码折叠更新
                        updateFolding(file);
                    }
                });
            }
        }, 300); // 300ms debounce
    }
    
    /**
     * 更新文件的代码折叠
     */
    private void updateFolding(PsiFile file) {
        if (file == null || !file.isValid() || file.getVirtualFile() == null) {
            return;
        }
        
        // 获取文件对应的编辑器
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor[] editors = fileEditorManager.getEditors(file.getVirtualFile());
        
        for (FileEditor fileEditor : editors) {
            if (fileEditor instanceof TextEditor) {
                Editor editor = ((TextEditor) fileEditor).getEditor();
                // 触发折叠区域更新
                CodeFoldingManager.getInstance(project).updateFoldRegions(editor);
            }
        }
    }
    
    private String getRelativePath(PsiFile file) {
        String basePath = project.getBasePath();
        String filePath = file.getVirtualFile().getPath();
        if (basePath != null && filePath.startsWith(basePath)) {
            return filePath.substring(basePath.length() + 1);
        }
        return filePath;
    }
    
    public void clearCache() {
        cache.clear();
    }
}
