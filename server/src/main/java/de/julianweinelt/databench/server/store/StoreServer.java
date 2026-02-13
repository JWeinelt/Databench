package de.julianweinelt.databench.server.store;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;

@Slf4j
public class StoreServer {
    private Javalin app;

    public void start() {
        app = Javalin.create(conf -> {
            conf.showJavalinBanner = false;
            conf.staticFiles.add("public-store", Location.EXTERNAL);
        }).error(404, ctx -> {
            ctx.html(Files.readString(new File("public-store/error/404.html").toPath()));
        }).error(503, ctx -> {
            ctx.html(Files.readString(new File("public-store/error/503.html").toPath()));
        })
        .start(7001);
    }
}