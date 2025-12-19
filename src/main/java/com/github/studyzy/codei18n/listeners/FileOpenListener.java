package com.github.studyzy.codei18n.listeners;

import com.github.studyzy.codei18n.settings.DisplayMode;
import com.github.studyzy.codei18n.settings.PluginSettings;
import com.github.studyzy.codei18n.utils.FileUtils;
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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

/**
 * File Editor Listener
 * Automatically folds translation comments when Go files are opened
 */
public class FileOpenListener implements FileEditorManagerListener {
    
    private static final Logger LOG = Logger.getInstance(FileOpenListener.class);
    
    // Use static Alarm because the listener may be instantiated multiple times
    private static final Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    
    public FileOpenListener() {
        // Parameterless constructor, for plugin.xml registration
    }
    
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        LOG.info("[DEBUG] FileOpenListener.fileOpened called for: " + file.getName());
        
        // Print file type and language information
        FileEditor[] editors = source.getEditors(file);
        if (editors.length > 0 && editors[0] instanceof TextEditor) {
            TextEditor textEditor = (TextEditor) editors[0];
            PsiFile psiFile = PsiManager.getInstance(source.getProject()).findFile(file);
            if (psiFile != null) {
                LOG.info("[DEBUG] File language ID: " + psiFile.getLanguage().getID());
                LOG.info("[DEBUG] File language display name: " + psiFile.getLanguage().getDisplayName());
            }
        }
        
        // Only process supported file types
        boolean isSupported = FileUtils.isSupportedFile(file.getName());
        LOG.info("[DEBUG] isSupportedFile: " + isSupported);
        
        if (!isSupported) {
            return;
        }
        
        PluginSettings settings = PluginSettings.getInstance();
        LOG.info("[DEBUG] Settings - enabled: " + settings.enabled + ", displayMode: " + settings.displayMode);
        
        // Check if the plugin is enabled and if it is in FOLDING mode
        if (!settings.enabled || settings.displayMode != DisplayMode.FOLDING) {
            return;
        }
        
        LOG.info("Supported file opened: " + file.getName() + ", scheduling fold collapse");
        
        Project project = source.getProject();
        
        // Delay the folding operation to wait for FoldingBuilder to complete constructing the folding regions
        // Multiple delayed attempts are needed because translation data may require time to fetch
        scheduleCollapseWithRetry(source, file, project, 0);
    }
    
    /**
     * Fold operation with retry
     */
    private void scheduleCollapseWithRetry(FileEditorManager manager, VirtualFile file, Project project, int attempt) {
        if (attempt >= 5) {
            LOG.info("Max retry attempts reached for file: " + file.getName());
            return;
        }
        
        int delay = attempt == 0 ? 500 : 1000; // First time 500ms, subsequent times 1000ms
        
        alarm.addRequest(() -> {
            if (project.isDisposed()) {
                return;
            }
            
            int collapsedCount = collapseTranslationFolds(manager, file);
            
            // If not collapsed into any region, it may be that the translation data is not ready yet, retry
            if (collapsedCount == 0 && attempt < 4) {
                LOG.info("No fold regions found yet, retrying... attempt=" + (attempt + 1));
                scheduleCollapseWithRetry(manager, file, project, attempt + 1);
            }
        }, delay);
    }
    
    /**
     * Collapse translation comment
     * @return Number of collapsed regions
     */
    private int collapseTranslationFolds(FileEditorManager manager, VirtualFile file) {
        FileEditor[] editors = manager.getEditors(file);
        int totalCollapsed = 0;
        
        for (FileEditor fileEditor : editors) {
            if (fileEditor instanceof TextEditor) {
                Editor editor = ((TextEditor) fileEditor).getEditor();
                FoldingModel foldingModel = editor.getFoldingModel();
                
                final int[] collapsedCount = {0};
                
                // Execute in the folding batch
                foldingModel.runBatchFoldingOperation(() -> {
                    FoldRegion[] regions = foldingModel.getAllFoldRegions();
                    
                    for (FoldRegion region : regions) {
                        // Check if it is a translation folding region
                        // Translated comments typically start with "// " and contain Chinese or other translated content
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
     * Determines if it is a translation placeholder
     */
    private boolean isTranslationPlaceholder(String placeholder) {
        if (placeholder == null || placeholder.isEmpty()) {
            return false;
        }
        
        // Exclude common non-translation placeholders
        if (placeholder.equals("...") || placeholder.equals("// ...") || placeholder.equals("/* ... */")) {
            return false;
        }
        
        // Check if it is in comment format
        if (placeholder.startsWith("// ") || placeholder.startsWith("/* ")) {
            // Check if it contains non-ASCII characters (such as Chinese, etc.)
            for (char c : placeholder.toCharArray()) {
                if (c > 127) {
                    return true; // Contains Chinese or other non-ASCII characters
                }
            }
        }
        
        return false;
    }
}