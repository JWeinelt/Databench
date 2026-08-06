package de.julianweinelt.datacat.flow;

import de.julianweinelt.datacat.dbx.api.DbxAPI;
import de.julianweinelt.datacat.dbx.api.drivers.DriverManagerService;
import de.julianweinelt.datacat.dbx.api.plugins.PluginLoader;
import de.julianweinelt.datacat.dbx.api.plugins.YarnLoader;
import de.julianweinelt.datacat.flow.flow.FlowCLI;
import de.julianweinelt.datacat.flow.flow.FlowServer;
import de.julianweinelt.datacat.flow.flow.FlowSocketServer;
import de.julianweinelt.datacat.flow.flow.auth.UserManager;
import de.julianweinelt.datacat.flow.job.JobAgent;
import de.julianweinelt.datacat.flow.setup.SetupManager;
import de.julianweinelt.datacat.flow.storage.DatabaseProvider;
import de.julianweinelt.datacat.flow.storage.LocalStorage;
import de.julianweinelt.datacat.flow.util.CryptoUtil;
import de.julianweinelt.datacat.flow.util.JWTUtil;
import de.julianweinelt.datacat.flow.util.SystemPlugin;
import de.julianweinelt.datacat.flow.util.UpdateChecker;
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

    @Getter
    private FlowServer server;
    @Getter
    private FlowSocketServer socketServer;
    @Getter
    private UserManager userManager;
    @Getter
    private CryptoUtil cryptoUtil;

    private YarnLoader yarnLoader;

    @Getter
    private DatabaseProvider databaseProvider;

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
        log.info("Starting DataCat Flow...");
        log.info("Loading drivers from disk...");
        try {
            driverManagerService.preloadDrivers();
        } catch (IOException e) {
            log.error("Failed to load drivers from disk (IO): {}", e.getMessage(), e);
        } catch (SQLException e) {
            log.error("An internal SQL error occurred loading drivers from disk: {}", e.getMessage(), e);
        }
        yarnLoader = new YarnLoader();
        log.info("Loading DBX plugins...");
        PluginLoader loader = new PluginLoader(api);
        loader.loadAll();
        storage = new LocalStorage(new File("config.json"));
        if (!storage.configCreated()) {
            loader.initializeDatabasePlugins();
            new SetupManager().startCLI();
            return;
        }
        log.info("Loading local configuration data...");
        storage.load();
        new JWTUtil();
        cryptoUtil = new CryptoUtil(LocalStorage.instance().getConfig().getEncryptionPassword());
        new JobAgent();
        userManager = new UserManager();
        socketServer = new FlowSocketServer();

        server = new FlowServer();
        server.start();
        databaseProvider = new DatabaseProvider();
        databaseProvider.start();
        loader.initializeAll();

        new Thread(() -> new FlowCLI().start()).start();
    }

    public void stop() {
        server.stop();
        try {
            socketServer.stop();
        } catch (InterruptedException e) {
            log.error("Failed to stop socket server: {}", e.getMessage(), e);
        }
    }

    public void restart() {
        stop();
        instance = new Flow();
        instance.start();
    }
}