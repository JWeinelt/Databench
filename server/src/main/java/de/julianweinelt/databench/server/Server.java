package de.julianweinelt.databench.server;

import de.julianweinelt.databench.server.server.WebServer;
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
        prepareDirs();

        versionManager = new VersionManager();
        versionManager.load();
        versionManager.loadLatestVersions();

        webServer = new WebServer();
        webServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(webServer::stop));
    }

    private void prepareDirs() {
        new File("files").mkdirs();
        new File("files/worker").mkdirs();
        new File("files/launcher").mkdirs();
        new File("files/ui").mkdirs();
    }
}