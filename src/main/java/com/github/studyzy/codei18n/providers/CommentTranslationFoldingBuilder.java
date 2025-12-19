package com.github.studyzy.codei18n.providers;

import com.github.studyzy.codei18n.models.TranslatedComment;
import com.github.studyzy.codei18n.services.TranslationService;
import com.github.studyzy.codei18n.settings.DisplayMode;
import com.github.studyzy.codei18n.settings.PluginSettings;
import com.github.studyzy.codei18n.utils.FileUtils;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comment Translation Folding Builder
 * Fold English comments to display Chinese translations, users can expand to view the original English text
 */
public class CommentTranslationFoldingBuilder extends FoldingBuilderEx {
    
    private static final Logger LOG = Logger.getInstance(CommentTranslationFoldingBuilder.class);
    private static final FoldingGroup TRANSLATION_GROUP = FoldingGroup.newGroup("comment-translation");
    
    // Cache translation results for the getPlaceholderText method
    private static final ConcurrentHashMap<Integer, String> translationCache = new ConcurrentHashMap<>();
    
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(
            @NotNull PsiElement root,
            @NotNull Document document,
            boolean quick
    ) {
        PluginSettings settings = PluginSettings.getInstance();
        
        // If settings are not initialized, return an empty array (for test environments)
        if (settings == null) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        // If the plugin is not enabled, return an empty array
        if (!settings.enabled) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        // If not in FOLDING mode, return an empty array
        if (settings.displayMode != DisplayMode.FOLDING) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        // Only process supported file types (Go, Rust, Java, etc.)
        if (!isSupportedFile(root)) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        
        // Note: We process in both quick and full mode to ensure translations are shown immediately
        // The TranslationService uses caching to avoid performance issues
        
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        
        try {
            String filename = root.getContainingFile().getName();
            String languageId = root.getContainingFile().getLanguage().getID();
            LOG.warn("[DEBUG] CommentTranslationFoldingBuilder.buildFoldRegions called for: " + filename);
            LOG.warn("[DEBUG] Language ID: " + languageId);
            LOG.warn("[DEBUG] Settings - enabled: " + settings.enabled + ", displayMode: " + settings.displayMode);
            LOG.warn("[DEBUG] isSupportedFile: " + isSupportedFile(root));
            
            // Using synchronous method to fetch translation data
            TranslationService translationService = TranslationService.getInstance(root.getProject());
            List<TranslatedComment> translations = translationService.getTranslationsSync(
                root.getContainingFile(), 
                5000  // 5-second timeout
            );
            
            LOG.warn("[DEBUG] Got " + translations.size() + " translations for " + filename);
            
            if (translations.isEmpty()) {
                LOG.warn("[DEBUG] No translations found for file: " + root.getContainingFile().getName());
                return FoldingDescriptor.EMPTY_ARRAY;
            }
            
            LOG.warn("[DEBUG] Building fold regions for " + root.getContainingFile().getName() + 
                    ", got " + translations.size() + " translations");
            
            // Iterate directly through translation results, decoupled from PSI structure
            // This ensures robustness even in environments with incomplete language support (e.g., missing Java parser in RustRover)
            for (TranslatedComment translation : translations) {
                // Check if range is valid
                if (translation.startOffset() < 0 || translation.endOffset() > document.getTextLength()) {
                    LOG.warn("[DEBUG] Invalid range: " + translation.startOffset() + "-" + translation.endOffset() + ", doc length: " + document.getTextLength());
                    continue;
                }
                
                String placeholderText = formatTranslation(translation.sourceText(), translation.translation());
                
                LOG.warn("[DEBUG] Creating fold region: offset=" + translation.startOffset() + "-" + translation.endOffset() + 
                        ", placeholder=" + placeholderText.substring(0, Math.min(50, placeholderText.length())));
                
                // Cache translation results
                translationCache.put(translation.startOffset(), placeholderText);
                
                TextRange textRange = new TextRange(translation.startOffset(), translation.endOffset());
                
                // Use root node as the anchor
                FoldingDescriptor descriptor = new FoldingDescriptor(
                    root.getNode(),
                    textRange,
                    TRANSLATION_GROUP,
                    placeholderText
                );
                descriptors.add(descriptor);
                
                LOG.warn("[DEBUG] Added fold region at offset " + translation.startOffset() + "-" + translation.endOffset());
            }
            
            LOG.warn("[DEBUG] Total fold descriptors created: " + descriptors.size());
            
        } catch (Exception e) {
            LOG.error("Failed to build fold regions", e);
        }
        
        return descriptors.toArray(new FoldingDescriptor[0]);
    }
    
    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        // This method might not be called if we use root node for descriptors
        // But for completeness, we check if there is a cached translation at the start offset
        // For custom FoldingDescriptors, the placeholder text is often passed in the constructor
        // However, if the IDE calls this, we try to return cached text
        
        // Note: When using FoldingDescriptor constructor with placeholder text, this method is usually skipped
        // But if we used the constructor without placeholder text, this would be called.
        // We used the constructor WITH placeholder text, so this implementation is fallback.
        return "...";
    }
    
    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        // Default collapsed (showing Chinese translation)
        return true;
    }
    
    /**
     * Format translation text, preserving comment symbols
     * If the translation text already contains comment symbols, do not add them again
     */
    private String formatTranslation(String originalComment, String translation) {
        if (translation == null) return "...";
        
        // Remove leading and trailing whitespace from the translated text
        String trimmedTranslation = translation.trim();
        
        if (originalComment.startsWith("///")) {
            if (trimmedTranslation.startsWith("///")) {
                return trimmedTranslation;
            }
            return "/// " + trimmedTranslation;
        } else if (originalComment.startsWith("//!")) {
            if (trimmedTranslation.startsWith("//!")) {
                return trimmedTranslation;
            }
            return "//! " + trimmedTranslation;
        } else if (originalComment.startsWith("//")) {
            // Check if the translation already starts with //
            if (trimmedTranslation.startsWith("//")) {
                return trimmedTranslation;
            }
            return "// " + trimmedTranslation;
        } else if (originalComment.startsWith("/**") && originalComment.endsWith("*/")) { // JSDoc
             if (trimmedTranslation.startsWith("/**") && trimmedTranslation.endsWith("*/")) {
                return trimmedTranslation;
            }
            return "/** " + trimmedTranslation + " */";
        } else if (originalComment.startsWith("/*") && originalComment.endsWith("*/")) {
            // Check if the translation is already in block comment format
            if (trimmedTranslation.startsWith("/*") && trimmedTranslation.endsWith("*/")) {
                return trimmedTranslation;
            }
            return "/* " + trimmedTranslation + " */";
        }
        return trimmedTranslation;
    }
    
    /**
     * Check if it is a supported file type
     */
    private boolean isSupportedFile(PsiElement root) {
        String filename = root.getContainingFile().getName();
        return FileUtils.isSupportedFile(filename);
    }
}
