package com.github.studyzy.codei18n.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;

public class PluginSettingsConfigurable implements Configurable {
    private PluginSettingsPanel panel;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "CodeI18n";
    }

    @Override
    public @Nullable JComponent createComponent() {
        panel = new PluginSettingsPanel();
        reset();
        return panel.getPanel();
    }

    @Override
    public boolean isModified() {
        if (panel == null) return false;
        PluginSettings settings = PluginSettings.getInstance();
        return panel.isModified(settings);
    }

    @Override
    public void apply() {
        if (panel != null) {
            PluginSettings settings = PluginSettings.getInstance();
            panel.apply(settings);
        }
    }

    @Override
    public void reset() {
        if (panel != null) {
            PluginSettings settings = PluginSettings.getInstance();
            panel.reset(settings);
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
    }
}
