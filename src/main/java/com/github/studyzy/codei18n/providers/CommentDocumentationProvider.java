package com.github.studyzy.codei18n.providers;

import com.github.studyzy.codei18n.models.TranslatedComment;
import com.github.studyzy.codei18n.services.TranslationService;
import com.github.studyzy.codei18n.settings.PluginSettings;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Comment Documentation Provider
 * Displays the original English text when hovering over translated comments
 */
public class CommentDocumentationProvider extends AbstractDocumentationProvider {
    
    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        // If the plugin is not enabled, return null
        if (!PluginSettings.getInstance().enabled) {
            return null;
        }
        
        // Check if it is a comment
        if (element instanceof PsiComment) {
            PsiComment comment = (PsiComment) element;
            
            // Attempt to retrieve the original English comment from the translation service
            String originalText = getOriginalText(comment);
            
            if (originalText != null && !originalText.isEmpty()) {
                String cleanText = cleanCommentText(originalText);
                // Returns formatted documentation
                return formatDocumentation("英文原文", cleanText);
            }
            
            // If there is no translation data, directly display the original comment text
            String commentText = comment.getText();
            String cleanText = cleanCommentText(commentText);
            return formatDocumentation("注释内容", cleanText);
        }
        
        return null;
    }
    
    /**
     * Get the original English text of the comment
     */
    private String getOriginalText(PsiComment comment) {
        try {
            TranslationService translationService = TranslationService.getInstance(comment.getProject());
            List<TranslatedComment> translations = translationService.getTranslations(
                comment.getContainingFile(),
                false
            );
            
            int startOffset = comment.getTextRange().getStartOffset();
            
            for (TranslatedComment translatedComment : translations) {
                if (translatedComment.startOffset() == startOffset) {
                    // Returns the stored source text (English original)
                    String sourceText = translatedComment.sourceText();
                    if (sourceText != null && !sourceText.isEmpty()) {
                        return sourceText;
                    }
                    // If sourceText is empty, return the text of the comment itself
                    return comment.getText();
                }
            }
        } catch (Exception e) {
            // Ignore errors, return the comment's own text as fallback
            return comment.getText();
        }
        
        return null;
    }
    
    @Override
    public @Nullable PsiElement getCustomDocumentationElement(
            @NotNull Editor editor,
            @NotNull PsiFile file,
            @Nullable PsiElement contextElement,
            int targetOffset
    ) {
        // If the cursor is on a comment, return the comment element
        if (contextElement instanceof PsiComment) {
            return contextElement;
        }
        
        // Check if the parent element is a comment
        PsiElement parent = contextElement != null ? contextElement.getParent() : null;
        if (parent instanceof PsiComment) {
            return parent;
        }
        
        return null;
    }
    
    /**
     * Cleans comment text, removes comment symbols
     */
    private String cleanCommentText(String commentText) {
        if (commentText.startsWith("//")) {
            return commentText.substring(2).trim();
        } else if (commentText.startsWith("/*") && commentText.endsWith("*/")) {
            return commentText.substring(2, commentText.length() - 2).trim();
        }
        return commentText.trim();
    }
    
    /**
     * Format document content as HTML
     */
    private String formatDocumentation(String title, String content) {
        return "<html><body>" +
                "<div style='margin: 5px;'>" +
                "<b>" + escapeHtml(title) + ":</b><br/>" +
                "<div style='margin-top: 5px; padding: 5px; background-color: #f5f5f5; border-radius: 3px;'>" +
                "<code>" + escapeHtml(content) + "</code>" +
                "</div>" +
                "</div>" +
                "</body></html>";
    }
    
    /**
     * HTML escape
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
