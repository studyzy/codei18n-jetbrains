package com.github.studyzy.codei18n.providers;

import com.github.studyzy.codei18n.models.TranslatedComment;
import com.github.studyzy.codei18n.services.TranslationService;
import com.github.studyzy.codei18n.settings.PluginSettings;
import com.github.studyzy.codei18n.utils.FileUtils;
import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 翻译 Inlay Hints 提供者
 * 在注释位置显示中文翻译，隐藏英文原文
 */
@SuppressWarnings("UnstableApiUsage")
public class TranslationInlayHintsProvider implements InlayHintsProvider<NoSettings> {
    
    private static final Logger LOG = Logger.getInstance(TranslationInlayHintsProvider.class);
    private static final SettingsKey<NoSettings> KEY = new SettingsKey<>("codei18n.translation.hints");
    
    @Override
    public boolean isVisibleInSettings() {
        return true;
    }
    
    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return KEY;
    }
    
    @NotNull
    @Override
    public String getName() {
        return "Code I18n Translation";
    }
    
    @Nullable
    @Override
    public String getPreviewText() {
        return "// Calculate sum\nfunc add(a, b int) int {\n    return a + b\n}";
    }
    
    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings settings) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener listener) {
                return new JPanel();
            }
        };
    }
    
    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }
    
    @NotNull
    @Override
    public InlayHintsCollector getCollectorFor(
            @NotNull PsiFile file,
            @NotNull Editor editor,
            @NotNull NoSettings settings,
            @NotNull InlayHintsSink sink
    ) {
        return new TranslationInlayCollector(editor, file);
    }
    
    /**
     * Inlay Hints 收集器
     */
    private static class TranslationInlayCollector extends FactoryInlayHintsCollector {
        
        private final Editor editor;
        private final PsiFile file;
        
        public TranslationInlayCollector(@NotNull Editor editor, @NotNull PsiFile file) {
            super(editor);
            this.editor = editor;
            this.file = file;
        }
        
        @Override
        public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
            // 如果插件未启用，不收集
            if (!PluginSettings.getInstance().enabled) {
                return true;
            }
            
            // 只处理支持的文件类型
            if (!FileUtils.isSupportedFile(file.getName())) {
                return true;
            }
            
            try {
                // 获取翻译数据
                TranslationService translationService = TranslationService.getInstance(file.getProject());
                List<TranslatedComment> translations = translationService.getTranslations(file, false);
                
                if (translations.isEmpty()) {
                    return true;
                }
                
                LOG.info("Collecting inlay hints for " + file.getName() + ", got " + translations.size() + " translations");
                
                // 遍历所有注释
                element.accept(new PsiRecursiveElementVisitor() {
                    @Override
                    public void visitComment(@NotNull PsiComment comment) {
                        super.visitComment(comment);
                        
                        int startOffset = comment.getTextRange().getStartOffset();
                        
                        // 查找匹配的翻译
                        for (TranslatedComment translatedComment : translations) {
                            if (translatedComment.startOffset() == startOffset) {
                                String translation = translatedComment.translation();
                                
                                if (translation != null && !translation.isEmpty()) {
                                    // 格式化翻译文本（保留注释符号）
                                    String displayText = formatTranslation(comment.getText(), translation);
                                    
                                    // 创建翻译显示
                                    InlayPresentation presentation = createTranslationPresentation(
                                        displayText,
                                        comment.getText()
                                    );
                                    
                                    // 在注释开始位置添加 inline inlay
                                    sink.addInlineElement(
                                        startOffset,
                                        false, // relatesToPrecedingText
                                        presentation,
                                        false  // showAbove
                                    );
                                    
                                    LOG.info("Added inlay hint at offset " + startOffset + ": " + displayText);
                                }
                                break;
                            }
                        }
                    }
                });
                
            } catch (Exception e) {
                LOG.error("Failed to collect inlay hints", e);
            }
            
            return true;
        }
        
        /**
         * 创建翻译显示
         */
        private InlayPresentation createTranslationPresentation(String translation, String originalText) {
            PresentationFactory factory = getFactory();
            
            // 创建中文翻译文本
            InlayPresentation textPresentation = factory.text(translation);
            
            // 添加工具提示（显示英文原文）
            InlayPresentation withTooltip = factory.withTooltip(
                "英文原文: " + originalText,
                textPresentation
            );
            
            // 添加点击事件（可选：点击切换显示）
            InlayPresentation clickable = factory.referenceOnHover(
                withTooltip,
                (event, translated) -> {
                    // 可以在这里添加点击切换逻辑
                }
            );
            
            return clickable;
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
            } else if (originalComment.startsWith("/**") && originalComment.endsWith("*/")) { // JSDoc
                return "/** " + translation + " */";
            } else if (originalComment.startsWith("/*") && originalComment.endsWith("*/")) {
                return "/* " + translation + " */";
            }
            return translation;
        }
    }
}
