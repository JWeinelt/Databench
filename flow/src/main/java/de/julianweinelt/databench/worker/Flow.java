package de.julianweinelt.databench.worker;

import de.julianweinelt.databench.dbx.api.DbxAPI;
import de.julianweinelt.databench.dbx.api.plugins.PluginLoader;
import de.julianweinelt.databench.worker.setup.SetupManager;
import de.julianweinelt.databench.worker.storage.LocalStorage;
import de.julianweinelt.databench.worker.util.SystemPlugin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class Flow {
    private static Flow instance;

    private DbxAPI api;
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
        api = new DbxAPI(new File("api"));
        log.info("Welcome!");
        log.info("Starting DataBench Flow...");
        storage = new LocalStorage(new File("config.json"));
        if (!storage.configCreated()) {
            new SetupManager().startCLI();
            return;
        }
        log.info("Loading local configuration data...");
        storage.load();
        log.info("Loading DBX plugins...");
        PluginLoader loader = new PluginLoader(api.getRegistry(), new SystemPlugin());
        loader.loadAll();
    }
}
