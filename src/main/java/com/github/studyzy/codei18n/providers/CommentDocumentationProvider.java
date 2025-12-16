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
 * 注释文档提供者
 * 在鼠标悬停在翻译后的注释上时，显示英文原文
 */
public class CommentDocumentationProvider extends AbstractDocumentationProvider {
    
    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        // 如果插件未启用，返回 null
        if (!PluginSettings.getInstance().enabled) {
            return null;
        }
        
        // 检查是否为注释
        if (element instanceof PsiComment) {
            PsiComment comment = (PsiComment) element;
            
            // 尝试从翻译服务获取原始英文注释
            String originalText = getOriginalText(comment);
            
            if (originalText != null && !originalText.isEmpty()) {
                String cleanText = cleanCommentText(originalText);
                // 返回格式化的文档
                return formatDocumentation("英文原文", cleanText);
            }
            
            // 如果没有翻译数据，直接显示注释原文
            String commentText = comment.getText();
            String cleanText = cleanCommentText(commentText);
            return formatDocumentation("注释内容", cleanText);
        }
        
        return null;
    }
    
    /**
     * 获取注释的原始英文文本
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
                if (translatedComment.getStartOffset() == startOffset) {
                    // 返回存储的源文本（英文原文）
                    String sourceText = translatedComment.getSourceText();
                    if (sourceText != null && !sourceText.isEmpty()) {
                        return sourceText;
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // 忽略错误，返回 null 使用默认行为
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
        // 如果光标在注释上，返回注释元素
        if (contextElement instanceof PsiComment) {
            return contextElement;
        }
        
        // 检查父元素是否为注释
        PsiElement parent = contextElement != null ? contextElement.getParent() : null;
        if (parent instanceof PsiComment) {
            return parent;
        }
        
        return null;
    }
    
    /**
     * 清理注释文本，移除注释符号
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
     * 格式化文档内容为 HTML
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
     * HTML 转义
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
