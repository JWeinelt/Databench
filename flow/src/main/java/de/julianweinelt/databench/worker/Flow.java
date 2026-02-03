package de.julianweinelt.databench.worker;

import de.julianweinelt.databench.dbx.api.DbxAPI;
import de.julianweinelt.databench.dbx.api.drivers.DriverManagerService;
import de.julianweinelt.databench.dbx.api.plugins.PluginLoader;
import de.julianweinelt.databench.worker.setup.SetupManager;
import de.julianweinelt.databench.worker.storage.LocalStorage;
import de.julianweinelt.databench.worker.util.SystemPlugin;
import de.julianweinelt.databench.worker.util.UpdateChecker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

@Slf4j
public class Flow {
    public static final String version = "1.0.1";
    private static Flow instance;

    @Getter
    private DbxAPI api;
    @Getter
    private DriverManagerService driverManagerService;
    @Getter
    private LocalStorage storage;

    public static void main(String[] args) {
        instance = new Flow();
        instance.start();
    }

    public static Flow instance() {
        return instance;
    }

    private void start() {
        api = new DbxAPI(new File("api"), new SystemPlugin());
        driverManagerService = new DriverManagerService();
        log.info("Checking for updates...");
        new UpdateChecker().checkForUpdates(true);
        log.info("Welcome!");
        log.info("Starting DataBench Flow...");
        log.info("Loading drivers from disk...");
        try {
            driverManagerService.preloadDrivers();
        } catch (IOException e) {
            log.error("Failed to load drivers from disk (IO): {}", e.getMessage(), e);
        } catch (SQLException e) {
            log.error("An internal SQL error occurred loading drivers from disk: {}", e.getMessage(), e);
        }
        storage = new LocalStorage(new File("config.json"));
        if (!storage.configCreated()) {
            new SetupManager().startCLI();
            return;
        }
        log.info("Loading local configuration data...");
        storage.load();
        log.info("Loading DBX plugins...");
        PluginLoader loader = new PluginLoader(api);
        loader.loadAll();
    }
}