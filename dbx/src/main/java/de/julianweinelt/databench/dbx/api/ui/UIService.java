package de.julianweinelt.databench.dbx.api.ui;

import de.julianweinelt.databench.dbx.api.ui.menubar.MenuManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UIService {
    @Getter
    private final List<SettingsPanel> settingsPanels = new ArrayList<>();

    @Getter
    private final MenuManager menuManager;
    @Getter
    private final ShortcutManager shortcutManager;

    private static UIService instance;
    public static UIService instance() {
        return instance;
    }

    public UIService() {
        instance = this;
        log.info("UIService instance created");
        shortcutManager = new ShortcutManager();
        menuManager = new MenuManager();
    }

    public void addSettingsPanel(SettingsPanel settingsPanel) {
        settingsPanels.add(settingsPanel);
    }
}