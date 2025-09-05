package de.julianweinelt.databench;

import de.julianweinelt.databench.data.ConfigManager;
import de.julianweinelt.databench.ui.BenchUI;
import lombok.Getter;

public class DataBench {
    @Getter
    private BenchUI ui;

    @Getter
    private static DataBench instance;

    @Getter
    private ConfigManager configManager;

    public static void main(String[] args) {
        instance = new DataBench();
        instance.start();
    }

    public void start() {
        configManager = new ConfigManager();
        configManager.loadConfig();
        ui = new BenchUI();
        ui.start();
    }
}
