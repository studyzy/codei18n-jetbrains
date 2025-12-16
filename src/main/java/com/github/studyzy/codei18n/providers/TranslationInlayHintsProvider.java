package com.github.studyzy.codei18n.providers;

import com.github.studyzy.codei18n.models.TranslatedComment;
import com.github.studyzy.codei18n.services.TranslationService;
import com.github.studyzy.codei18n.settings.PluginSettings;
import com.intellij.codeInsight.hints.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class TranslationInlayHintsProvider implements InlayHintsProvider<NoSettings> {

    private static final SettingsKey<NoSettings> KEY = new SettingsKey<>("codei18n.hints");

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile file,
                                               @NotNull Editor editor,
                                               @NotNull NoSettings settings,
                                               @NotNull InlayHintsSink sink) {
        return new FactoryInlayHintsCollector(editor) {
            @Override
            public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
                if (!PluginSettings.getInstance().enabled) return true;

                if (element instanceof PsiComment) {
                    List<TranslatedComment> translations = TranslationService.getInstance(file.getProject())
                            .getTranslations(file, false);
                    
                    int startOffset = element.getTextRange().getStartOffset();
                    int endOffset = element.getTextRange().getEndOffset();

                    for (TranslatedComment comment : translations) {
                        // Match roughly by offset (CLI range vs PSI range)
                        // Precise match might be tricky due to whitespace or comment symbols.
                        // Let's check overlap or if comment contains the translated range.
                        
                        // TranslationService calculated startOffset based on line/col.
                        // Ideally they match.
                        
                        if (comment.getStartOffset() == startOffset) {
                             addHint(sink, element.getTextRange().getEndOffset(), comment.getTranslation());
                             break;
                        }
                    }
                }
                return true;
            }

            private void addHint(InlayHintsSink sink, int offset, String text) {
                sink.addInlineElement(
                        offset,
                        true,
                        getFactory().smallText(text),
                        false
                );
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
    public String getName() {
        return "CodeI18n Translations";
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return "// Calculate sum\n// 计算总和";
    }

    @Override
    public boolean isImmediate() {
        return true;
    }
}
