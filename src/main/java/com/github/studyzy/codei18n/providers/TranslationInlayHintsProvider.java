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
 * Inlay Hints Provider for Translation
 * Displays Chinese translation at comment positions and hides the original English text
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
     * Inlay Hints Collector
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
            // If the plugin is not enabled, do not collect
            if (!PluginSettings.getInstance().enabled) {
                return true;
            }
            
            // Only process supported file types
            if (!FileUtils.isSupportedFile(file.getName())) {
                return true;
            }
            
            try {
                // Get translation data
                TranslationService translationService = TranslationService.getInstance(file.getProject());
                List<TranslatedComment> translations = translationService.getTranslations(file, false);
                
                if (translations.isEmpty()) {
                    return true;
                }
                
                LOG.info("Collecting inlay hints for " + file.getName() + ", got " + translations.size() + " translations");
                
                // Iterate through all comments
                element.accept(new PsiRecursiveElementVisitor() {
                    @Override
                    public void visitComment(@NotNull PsiComment comment) {
                        super.visitComment(comment);
                        
                        int startOffset = comment.getTextRange().getStartOffset();
                        
                        // Find matching translation
                        for (TranslatedComment translatedComment : translations) {
                            if (translatedComment.startOffset() == startOffset) {
                                String translation = translatedComment.translation();
                                
                                if (translation != null && !translation.isEmpty()) {
                                    // Format translation text (preserve comment symbols)
                                    String displayText = formatTranslation(comment.getText(), translation);
                                    
                                    // Create translation display
                                    InlayPresentation presentation = createTranslationPresentation(
                                        displayText,
                                        comment.getText()
                                    );
                                    
                                    // Add inline inlay at the beginning of the comment
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
         * Create translation display
         */
        private InlayPresentation createTranslationPresentation(String translation, String originalText) {
            PresentationFactory factory = getFactory();
            
            // Create Chinese translation text
            InlayPresentation textPresentation = factory.text(translation);
            
            // Add tooltip (display original English text)
            InlayPresentation withTooltip = factory.withTooltip(
                "英文原文: " + originalText,
                textPresentation
            );
            
            // Add click event (optional: toggle display on click)
            InlayPresentation clickable = factory.referenceOnHover(
                withTooltip,
                (event, translated) -> {
                    // Click toggle logic can be added here
                }
            );
            
            return clickable;
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
}
