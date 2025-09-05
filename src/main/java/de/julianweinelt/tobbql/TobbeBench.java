package de.julianweinelt.tobbql;

import de.julianweinelt.tobbql.data.ConfigManager;
import de.julianweinelt.tobbql.ui.TobbeUI;
import lombok.Getter;

public class TobbeBench {
    @Getter
    private TobbeUI ui;

    @Getter
    private static TobbeBench instance;

    @Getter
    private ConfigManager configManager;

    public static void main(String[] args) {
        instance = new TobbeBench();
        instance.start();
    }

    public void start() {
        configManager = new ConfigManager();
        configManager.loadConfig();
        ui = new TobbeUI();
        ui.start();
    }
}
