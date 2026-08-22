package de.julianweinelt.datacat.server.server;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import de.julianweinelt.datacat.server.util.JWTUtil;
import io.javalin.Javalin;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class StoreServer {
    @Setter
    private boolean maintenance = false;
    private Javalin app;

    private String maintenanceHtml;

    private static final Map<String, String> MIME_TYPES = Map.of(
            ".html", "text/html",
            ".css", "text/css",
            ".js", "application/javascript",
            ".json", "application/json",
            ".svg", "image/svg+xml",
            ".png", "image/png",
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".gif", "image/gif",
            ".ico", "image/x-icon"
    );

    private final List<String> authRequiredApiRoutes = List.of(
            "/api/account/settings"
    );

    public void start() {
        app = Javalin.create(config -> {
            config.startup.showJavalinBanner = false;
            config.startup.startupWatcherEnabled = false;
            config.startup.showOldJavalinVersionWarning = false;
            config.staticFiles.add("public-store", Location.EXTERNAL);

            RoutesConfig r = config.routes;
            r.before(ctx -> {
                String path = ctx.path();
                if (path.startsWith("/api")) {
                    if (maintenance) {
                        applyError(ctx, Error.MAINTENANCE);
                        ctx.status(HttpStatus.SERVICE_UNAVAILABLE);
                        ctx.skipRemainingHandlers();
                    } else {
                        if (authRequiredApiRoutes.contains(path)) {

                        }
                    }
                } else {
                    if (maintenance) {
                        ctx.result(maintenanceHtml);
                    }
                }
            });
            defineAPI(r);
            defineExtraRoutes(r);
        });

        app.start();
    }
    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    private void readMaintenanceHtml() {
        try {
            maintenanceHtml = Files.readString(Path.of("public-store", "maintenance", "index.html"));
        } catch (IOException e) {
            maintenanceHtml = "<p>An unknown error occurred. Code: 81";
        }
    }

    public boolean isInMaintenance() {
        return maintenance;
    }

    private void defineExtraRoutes(RoutesConfig r) {

    }

    private void defineAPI(RoutesConfig r) {
        r.post("/api/auth", ctx -> {
            String header = ctx.header("Authorization");
            if (header == null) {
                applyError(ctx, Error.INVALID_HEADER, HttpStatus.UNAUTHORIZED);
                return;
            }
            String base64 = header.replace("Basic ", "");
            String decoded = new String(Base64.getDecoder().decode(base64));
            if (!decoded.contains(":")) {
                applyError(ctx, Error.INVALID_HEADER, HttpStatus.UNAUTHORIZED);
                return;
            }
            String username = decoded.split(":")[0];
            String password = decoded.split(":")[1];

            //TODO: Authenticate
        });

        r.post("/api/auth/refresh", ctx -> {
            String tokenHeader = ctx.header("Authorization");
            if (tokenHeader == null) {
                applyError(ctx, Error.INVALID_HEADER, HttpStatus.UNAUTHORIZED);
                return;
            }
            String refreshToken = tokenHeader.replace("Bearer ", "");
            boolean valid = JWTUtil.instance().verifyRefresh(refreshToken, err -> {
                applyError(ctx, err, err);
                ctx.status(HttpStatus.UNAUTHORIZED);
            });
            if (!valid) return;

            DecodedJWT decodedJWT = JWTUtil.instance().decode(refreshToken);
            String t = JWTUtil.instance().token(decodedJWT.getSubject());
            String refresh = JWTUtil.instance().refreshToken(decodedJWT.getSubject());
            ctx.result(generateTokenBody(t, refresh).toString());
            ctx.status(200);
        });
    }

    private JsonObject generateTokenBody(String token, String refreshToken) {
        JsonObject body = new JsonObject();
        JsonObject t = new JsonObject();
        t.addProperty("scope", "accessToken");
        t.addProperty("token", token);
        JsonObject r = new JsonObject();
        r.addProperty("scope", "refreshToken");
        r.addProperty("token", refreshToken);
        body.add("accessToken", t);
        body.add("refreshToken", r);
        return body;
    }

    private void applyError(Context context, Error error) {
        JsonObject o = new JsonObject();
        o.addProperty("success", false);
        JsonObject e = new JsonObject();
        e.addProperty("title", error.title);
        e.addProperty("message", error.message);
        o.add("error", e);
        context.result(o.toString());
    }

    private void applyError(Context context, String title, String message) {
        JsonObject o = new JsonObject();
        o.addProperty("success", false);
        JsonObject e = new JsonObject();
        e.addProperty("title", title);
        e.addProperty("message", message);
        o.add("error", e);
        context.result(o.toString());
    }

    private void applyError(Context context, Error error, HttpStatus status) {
        applyError(context, error, status.getCode());
    }

    private void applyError(Context context, Error error, int status) {
        applyError(context, error);
        context.status(status);
    }

    public enum Error {
        MAINTENANCE("Maintenance work", "We're currently performing important maintenance work."),
        INVALID_HEADER("Invalid header", "The provided header either doesn't exist or is invalid.")
        ;

        public final String title;
        public final String message;

        Error(String title, String message) {
            this.title = title;
            this.message = message;
        }
    }
}