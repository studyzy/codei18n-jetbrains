package com.github.studyzy.codei18n.listeners;

import com.github.studyzy.codei18n.settings.DisplayMode;
import com.github.studyzy.codei18n.settings.PluginSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

/**
 * 文件编辑器监听器
 * 在 Go 文件打开时，自动折叠翻译注释
 */
public class FileOpenListener implements FileEditorManagerListener {
    
    private static final Logger LOG = Logger.getInstance(FileOpenListener.class);
    
    // 使用静态 Alarm，因为监听器可能被多次实例化
    private static final Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    
    public FileOpenListener() {
        // 无参构造函数，供 plugin.xml 注册使用
    }
    
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        // 支持 Go 和 Rust 文件
        String filename = file.getName();
        if (!filename.endsWith(".go") && !filename.endsWith(".rs")) {
            return;
        }
        
        PluginSettings settings = PluginSettings.getInstance();
        
        // 检查插件是否启用以及是否为 FOLDING 模式
        if (!settings.enabled || settings.displayMode != DisplayMode.FOLDING) {
            return;
        }
        
        LOG.info("Go file opened: " + file.getName() + ", scheduling fold collapse");
        
        Project project = source.getProject();
        
        // 延迟执行折叠操作，等待 FoldingBuilder 完成构建折叠区域
        // 需要多次延迟尝试，因为翻译数据可能需要时间获取
        scheduleCollapseWithRetry(source, file, project, 0);
    }
    
    /**
     * 带重试的折叠操作
     */
    private void scheduleCollapseWithRetry(FileEditorManager manager, VirtualFile file, Project project, int attempt) {
        if (attempt >= 5) {
            LOG.info("Max retry attempts reached for file: " + file.getName());
            return;
        }
        
        int delay = attempt == 0 ? 500 : 1000; // 首次500ms，后续1000ms
        
        alarm.addRequest(() -> {
            if (project.isDisposed()) {
                return;
            }
            
            int collapsedCount = collapseTranslationFolds(manager, file);
            
            // 如果没有折叠到任何区域，可能是翻译数据还没准备好，重试
            if (collapsedCount == 0 && attempt < 4) {
                LOG.info("No fold regions found yet, retrying... attempt=" + (attempt + 1));
                scheduleCollapseWithRetry(manager, file, project, attempt + 1);
            }
        }, delay);
    }
    
    /**
     * 折叠翻译注释
     * @return 折叠的区域数量
     */
    private int collapseTranslationFolds(FileEditorManager manager, VirtualFile file) {
        FileEditor[] editors = manager.getEditors(file);
        int totalCollapsed = 0;
        
        for (FileEditor fileEditor : editors) {
            if (fileEditor instanceof TextEditor) {
                Editor editor = ((TextEditor) fileEditor).getEditor();
                FoldingModel foldingModel = editor.getFoldingModel();
                
                final int[] collapsedCount = {0};
                
                // 在折叠批处理中执行
                foldingModel.runBatchFoldingOperation(() -> {
                    FoldRegion[] regions = foldingModel.getAllFoldRegions();
                    
                    for (FoldRegion region : regions) {
                        // 检查是否为翻译折叠区域
                        // 翻译后的注释通常以 "// " 开头且包含中文或其他翻译内容
                        String placeholder = region.getPlaceholderText();
                        if (isTranslationPlaceholder(placeholder)) {
                            if (region.isExpanded()) {
                                region.setExpanded(false);
                                collapsedCount[0]++;
                            }
                        }
                    }
                });
                
                totalCollapsed += collapsedCount[0];
                LOG.info("Collapsed " + collapsedCount[0] + " translation fold regions for file: " + file.getName());
            }
        }
        
        return totalCollapsed;
    }
    
    /**
     * 判断是否为翻译占位符
     */
    private boolean isTranslationPlaceholder(String placeholder) {
        if (placeholder == null || placeholder.isEmpty()) {
            return false;
        }
        
        // 排除常见的非翻译占位符
        if (placeholder.equals("...") || placeholder.equals("// ...") || placeholder.equals("/* ... */")) {
            return false;
        }
        
        // 检查是否为注释格式
        if (placeholder.startsWith("// ") || placeholder.startsWith("/* ")) {
            // 检查是否包含非 ASCII 字符（中文等）
            for (char c : placeholder.toCharArray()) {
                if (c > 127) {
                    return true; // 包含中文或其他非 ASCII 字符
                }
            }
        }
        
        return false;
    }
}