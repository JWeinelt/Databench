package de.julianweinelt.databench.server.store;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

@Slf4j
public class StoreServer {
    private boolean maintenance = true;
    private Javalin app;

    public void start() {
        app = Javalin.create(conf -> {
            conf.showJavalinBanner = false;
            conf.staticFiles.add("public-store", Location.EXTERNAL);
        }).error(404, ctx -> {
            ctx.html(Files.readString(new File("public-store/error/404.html").toPath()));
        }).error(503, ctx -> {
            ctx.html(Files.readString(new File("public-store/error/503.html").toPath()));
        }).before(ctx -> {
                    if (!maintenance) {
                        return;
                    }

                    String path = ctx.path();

                    boolean isStaticFile = path.contains(".");

                    if (isStaticFile) {
                        return;
                    }

                    ctx.status(503);
                    ctx.contentType("text/html");
                    ctx.result(Files.readString(new File("public-store/maintenance/index.html").toPath()));
                    ctx.skipRemainingHandlers();
                })
        .start(7001);
    }
}