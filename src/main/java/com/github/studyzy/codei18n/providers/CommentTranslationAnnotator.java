package com.github.studyzy.codei18n.providers;

import com.github.studyzy.codei18n.models.TranslatedComment;
import com.github.studyzy.codei18n.services.TranslationService;
import com.github.studyzy.codei18n.settings.PluginSettings;
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
 * 注释翻译注解器
 * 显示中文翻译，隐藏英文原文
 */
public class CommentTranslationAnnotator implements Annotator {
    
    private static final Logger LOG = Logger.getInstance(CommentTranslationAnnotator.class);
    
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // 只处理注释元素
        if (!(element instanceof PsiComment)) {
            return;
        }
        
        // 如果插件未启用，不处理
        if (!PluginSettings.getInstance().enabled) {
            return;
        }
        
        // 支持 Go 和 Rust 文件
        String filename = element.getContainingFile().getName();
        if (!filename.endsWith(".go") && !filename.endsWith(".rs")) {
            return;
        }
        
        PsiComment comment = (PsiComment) element;
        
        try {
            // 获取翻译数据
            TranslationService translationService = TranslationService.getInstance(element.getProject());
            List<TranslatedComment> translations = translationService.getTranslations(
                element.getContainingFile(),
                false
            );
            
            int startOffset = comment.getTextRange().getStartOffset();
            
            // 查找匹配的翻译
            for (TranslatedComment translatedComment : translations) {
                if (translatedComment.startOffset() == startOffset) {
                    String translation = translatedComment.translation();
                    
                    if (translation != null && !translation.isEmpty()) {
                        // 格式化翻译文本
                        String displayText = formatTranslation(comment.getText(), translation);
                        
                        // 创建注释样式的文本属性
                        TextAttributes attributes = new TextAttributes();
                        attributes.setForegroundColor(new JBColor(
                            new Color(128, 128, 128),  // 灰色（Light 主题）
                            new Color(128, 128, 128)   // 灰色（Dark 主题）
                        ));
                        attributes.setFontType(Font.ITALIC);
                        
                        // 创建信息注解，显示中文翻译
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
     * 格式化翻译文本，保留注释符号
     */
    private String formatTranslation(String originalComment, String translation) {
        if (originalComment.startsWith("///")) {
            return "/// " + translation;
        } else if (originalComment.startsWith("//!")) {
            return "//! " + translation;
        } else if (originalComment.startsWith("//")) {
            return "// " + translation;
        } else if (originalComment.startsWith("/*") && originalComment.endsWith("*/")) {
            return "/* " + translation + " */";
        }
        return translation;
    }
}
