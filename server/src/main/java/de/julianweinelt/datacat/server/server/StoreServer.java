package de.julianweinelt.datacat.server.server;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class StoreServer {
    private Javalin app;

    public void start() {
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.startupWatcherEnabled = false;
            config.staticFiles.add("public-store", Location.EXTERNAL);
        });

        app.start();


    }
}
