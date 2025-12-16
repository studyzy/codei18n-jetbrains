package com.github.studyzy.codei18n.services;

import com.github.studyzy.codei18n.models.CliResponse;
import com.github.studyzy.codei18n.models.TranslatedComment;
import com.github.studyzy.codei18n.settings.PluginSettings;
import com.github.studyzy.codei18n.utils.JsonParser;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.Alarm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class TranslationService implements Disposable {
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

    public List<TranslatedComment> getTranslations(PsiFile file, boolean forceRefresh) {
        String relativePath = getRelativePath(file);

        if (!forceRefresh && cache.containsKey(relativePath)) {
            return cache.get(relativePath);
        }
        
        // Trigger background refresh if not in cache (or forced)
        // Return empty/stale for now to avoid blocking
        scheduleRefresh(file, relativePath);
        
        return cache.getOrDefault(relativePath, Collections.emptyList());
    }

    private void scheduleRefresh(PsiFile file, String relativePath) {
        debounceAlarm.cancelAllRequests();
        
        // Capture content in current thread (likely ReadAction or EDT)
        final String content = file.getText();
        
        debounceAlarm.addRequest(() -> {
            CliService cliService = CliService.getInstance(project);
            
            String json = cliService.scanFile(
                relativePath, 
                true, 
                true, 
                content
            );

            if (json == null) return;

            CliResponse response = JsonParser.parseCliResponse(json);
            if (response == null || response.getComments() == null) return;

            List<TranslatedComment> translations = new ArrayList<>();
            
            // Document access needs ReadAction
            ApplicationManager.getApplication().runReadAction(() -> {
                Document document = PsiDocumentManager.getInstance(project).getDocument(file);
                if (document != null) {
                    for (CliResponse.CommentData data : response.getComments()) {
                        if (data.getTranslation() != null && !data.getTranslation().isEmpty()) {
                            TranslatedComment comment = new TranslatedComment();
                            comment.setCommentId(data.getId());
                            comment.setSourceText(data.getSourceText());
                            comment.setTranslation(data.getTranslation());
                            
                            int startLine = data.getRange().getStartLine() - 1;
                            int startCol = data.getRange().getStartCol() - 1;
                            int endLine = data.getRange().getEndLine() - 1;
                            int endCol = data.getRange().getEndCol() - 1;
                            
                            if (startLine < document.getLineCount() && endLine < document.getLineCount()) {
                                 int startOffset = document.getLineStartOffset(startLine) + startCol;
                                 int endOffset = document.getLineStartOffset(endLine) + endCol;
                                 comment.setStartOffset(startOffset);
                                 comment.setEndOffset(endOffset);
                                 comment.setLineNumber(data.getRange().getStartLine());
                                 translations.add(comment);
                            }
                        }
                    }
                }
            });

            cache.put(relativePath, translations);
            
            // Refresh UI on EDT
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed() && file.isValid()) {
                    DaemonCodeAnalyzer.getInstance(project).restart(file);
                }
            });
            
        }, 300); // 300ms debounce
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
