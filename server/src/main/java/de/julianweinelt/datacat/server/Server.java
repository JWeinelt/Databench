package de.julianweinelt.datacat.server;

import de.julianweinelt.datacat.server.server.WebServer;
import de.julianweinelt.datacat.server.store.StoreServer;
import de.julianweinelt.datacat.server.store.YarnManager;
import de.julianweinelt.datacat.server.store.account.AccountManager;
import de.julianweinelt.datacat.server.store.database.DBStorage;
import de.julianweinelt.datacat.server.store.files.Configuration;
import de.julianweinelt.datacat.server.store.files.LocalStorage;
import de.julianweinelt.datacat.server.util.JWTUtil;
import lombok.Getter;

import java.io.File;

public class Server {
    @Getter
    private VersionManager versionManager;
    @Getter
    private WebServer webServer;

    @Getter
    private static Server instance;


    public static void main(String[] args) {
        instance = new Server();
        instance.start();
    }

    public void start() {
        KeyManager.generateKey(false);

        new LocalStorage();
        if (Configuration.instance().getJwtSecret() == null) {
            Configuration.instance().setJwtSecret(KeyManager.generateRandomKey());
            LocalStorage.instance().saveConfig();
        }
        new JWTUtil(Configuration.instance().getJwtSecret());

        prepareDirs();

        versionManager = new VersionManager();
        versionManager.load();
        versionManager.loadLatestVersions();

        new DBStorage();
        DBStorage.instance().loadMetaData();

        new AccountManager();
        new YarnManager();

        webServer = new WebServer();
        webServer.start();


        new StoreServer().start();

        Runtime.getRuntime().addShutdownHook(new Thread(webServer::stop));
    }

    private void prepareDirs() {
        new File("files").mkdirs();
        new File("files/worker").mkdirs();
        new File("files/launcher").mkdirs();
        new File("files/ui").mkdirs();
    }
}