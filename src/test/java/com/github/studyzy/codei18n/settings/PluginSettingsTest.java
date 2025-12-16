package com.github.studyzy.codei18n.settings;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PluginSettingsTest {

    @Test
    public void testDefaults() {
        PluginSettings settings = new PluginSettings();
        assertTrue(settings.enabled);
        assertEquals(DisplayMode.INLAY_HINT, settings.displayMode);
        assertEquals("codei18n", settings.cliConfiguration.getCliPath());
        assertEquals("zh-CN", settings.cliConfiguration.getTargetLanguage());
    }
}
