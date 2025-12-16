package com.github.studyzy.codei18n.settings;

import com.github.studyzy.codei18n.models.CliConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.APP)
@State(
    name = "CodeI18nSettings",
    storages = @Storage("codei18n.xml")
)
public final class PluginSettings implements PersistentStateComponent<PluginSettings> {
    
    public boolean enabled = true;
    public DisplayMode displayMode = DisplayMode.FOLDING;
    public CliConfiguration cliConfiguration = new CliConfiguration();
    public boolean cacheEnabled = true;
    public int maxCacheSize = 100;
    
    @Nullable
    @Override
    public PluginSettings getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull PluginSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    public static PluginSettings getInstance() {
        return ApplicationManager.getApplication().getService(PluginSettings.class);
    }
}
