package com.github.studyzy.codei18n.providers;

import com.github.studyzy.codei18n.models.TranslatedComment;
import com.github.studyzy.codei18n.services.TranslationService;
import com.github.studyzy.codei18n.settings.PluginSettings;
import com.github.studyzy.codei18n.utils.FileUtils;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

/**
 * Comment translation annotator
 * Displays Chinese translation, hides English original
 */
public class CommentTranslationAnnotator implements Annotator {
    
    private static final Logger LOG = Logger.getInstance(CommentTranslationAnnotator.class);
    
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // Only process comment elements
        if (!(element instanceof PsiComment)) {
            return;
        }
        
        // If the plugin is not enabled, do not process
        if (!PluginSettings.getInstance().enabled) {
            return;
        }
        
        // Only process supported file types
        if (!FileUtils.isSupportedFile(element.getContainingFile().getName())) {
            return;
        }
        
        PsiComment comment = (PsiComment) element;
        
        try {
            // Get translation data
            TranslationService translationService = TranslationService.getInstance(element.getProject());
            List<TranslatedComment> translations = translationService.getTranslations(
                element.getContainingFile(),
                false
            );
            
            int startOffset = comment.getTextRange().getStartOffset();
            
            // Find matching translation
            for (TranslatedComment translatedComment : translations) {
                if (translatedComment.startOffset() == startOffset) {
                    String translation = translatedComment.translation();
                    
                    if (translation != null && !translation.isEmpty()) {
                        // Format the translated text
                        String displayText = formatTranslation(comment.getText(), translation);
                        
                        // Create text attributes for comment style
                        TextAttributes attributes = new TextAttributes();
                        attributes.setForegroundColor(new JBColor(
                            new Color(128, 128, 128),  // Gray (Light theme)
                            new Color(128, 128, 128)   // Gray (Dark theme)
                        ));
                        attributes.setFontType(Font.ITALIC);
                        
                        // Create info annotation, display Chinese translation
                        holder.newAnnotation(HighlightSeverity.INFORMATION, displayText)
                            .range(comment.getTextRange())
                            .enforcedTextAttributes(attributes)
                            .tooltip("英文原文: " + comment.getText())
                            .create();
                        
                        LOG.info("Annotated comment at offset " + startOffset + " with translation: " + displayText);
                    }
                    break;
                }
            }
            
        } catch (Exception e) {
            LOG.error("Failed to annotate comment", e);
        }
    }
    
    /**
         * Format translation text while preserving comment symbols
         */
    private String formatTranslation(String originalComment, String translation) {
        if (originalComment.startsWith("///")) {
            return "/// " + translation;
        } else if (originalComment.startsWith("//!")) {
            return "//! " + translation;
        } else if (originalComment.startsWith("//")) {
            return "// " + translation;
        } else if (originalComment.startsWith("/**") && originalComment.endsWith("*/")) { // JSDoc
            return "/** " + translation + " */";
        } else if (originalComment.startsWith("/*") && originalComment.endsWith("*/")) {
            return "/* " + translation + " */";
        }
        return translation;
    }
}
