package com.github.studyzy.codei18n.settings;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import javax.swing.*;

public class PluginSettingsPanel {
    private final JPanel mainPanel;
    private final JBCheckBox enabledCheckBox = new JBCheckBox("Enable translation display");
    private final TextFieldWithBrowseButton cliPathField = new TextFieldWithBrowseButton();
    private final JBTextField targetLanguageField = new JBTextField();

    public PluginSettingsPanel() {
        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(enabledCheckBox)
                .addLabeledComponent(new JBLabel("CLI Path:"), cliPathField, 1, false)
                .addLabeledComponent(new JBLabel("Target Language:"), targetLanguageField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public boolean isModified(PluginSettings settings) {
        return enabledCheckBox.isSelected() != settings.enabled ||
               !cliPathField.getText().equals(settings.cliConfiguration.getCliPath()) ||
               !targetLanguageField.getText().equals(settings.cliConfiguration.getTargetLanguage());
    }

    public void apply(PluginSettings settings) {
        settings.enabled = enabledCheckBox.isSelected();
        settings.cliConfiguration.setCliPath(cliPathField.getText());
        settings.cliConfiguration.setTargetLanguage(targetLanguageField.getText());
    }

    public void reset(PluginSettings settings) {
        enabledCheckBox.setSelected(settings.enabled);
        cliPathField.setText(settings.cliConfiguration.getCliPath());
        targetLanguageField.setText(settings.cliConfiguration.getTargetLanguage());
    }
}
