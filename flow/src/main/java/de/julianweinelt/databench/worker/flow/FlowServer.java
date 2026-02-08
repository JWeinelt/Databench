package de.julianweinelt.databench.worker.flow;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.julianweinelt.databench.worker.Flow;
import de.julianweinelt.databench.worker.flow.auth.UserManager;
import de.julianweinelt.databench.worker.job.JobAgent;
import de.julianweinelt.databench.worker.storage.LocalStorage;
import de.julianweinelt.databench.worker.util.JWTUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class FlowServer {
    private Javalin app;

    public void start() {
        log.info("Starting endpoint...");
        String hostAddress = LocalStorage.instance().getConfig().getInternalServerAddress();
        int hostPort = LocalStorage.instance().getConfig().getInternalServerPort();
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.startupWatcherEnabled = false;
                })
                .get("/api/v1/hello", ctx -> {
                    new JsonResponse().success()
                            .add("systemTime", System.currentTimeMillis())
                            .add("flowVersion", Flow.version)
                            .add("hello", "world")
                            .apply(ctx);
                })
                .post("/api/v1/auth", ctx -> {
                    String authHeader = ctx.header("Authorization");
                    if (authHeader == null || !authHeader.startsWith("Basic ")) {
                        ctx.status(401);
                        ctx.header("WWW-Authenticate", "Basic realm=\"api\"");
                        return;
                    }

                    String base64Credentials = authHeader.substring("Basic ".length());
                    byte[] decoded = Base64.getDecoder().decode(base64Credentials);
                    String credentials = new String(decoded, StandardCharsets.UTF_8);

                    int separator = credentials.indexOf(':');
                    if (separator < 0) {
                        ctx.status(400);
                        return;
                    }

                    String username = credentials.substring(0, separator);
                    String password = credentials.substring(separator + 1);

                    boolean valid = UserManager.instance().verify(username, password);

                    if (!valid) {
                        ctx.status(401);
                        return;
                    }

                    String token = JWTUtil.instance().token(username);
                    new JsonResponse().success()
                            .add("token", token)
                            .add("refreshToken", JWTUtil.instance().refreshToken(token))
                            .apply(ctx);
                })
                .post("/api/v1/refresh", ctx -> {
                    String refreshToken = ctx.header("Authorization");
                    if (refreshToken == null) {
                        new JsonResponse().error(ErrorType.TOKEN_MISSING).apply(ctx);
                        return;
                    }
                    String token = refreshToken.replace("Bearer ", "");
                    boolean valid = JWTUtil.instance().verifyRefresh(token, onError -> {
                        new JsonResponse().error(ErrorType.TOKEN_INVALID).add("additionalInfo", onError)
                                .apply(ctx);
                    });
                    if (!valid) return;
                    String oldInvalidToken = JWTUtil.instance().getUsername(token);
                    String userName = JWTUtil.instance().getUsername(oldInvalidToken);
                    String newToken = JWTUtil.instance().token(userName);
                    String newRToken = JWTUtil.instance().refreshToken(newToken);
                    new JsonResponse().success()
                            .add("token", newToken)
                            .add("refreshToken", newRToken)
                            .apply(ctx);
                })
                .before(ctx -> {
                    if (ctx.path().startsWith("/api/v1/flow/")) {
                        String token = ctx.header("Authorization");
                        if (token == null) {
                            new JsonResponse().error(ErrorType.TOKEN_MISSING).apply(ctx);
                            ctx.skipRemainingHandlers();
                            return;
                        }
                        token = token.replace("Bearer ", "");
                        boolean verified = JWTUtil.instance().verify(token, onError -> {
                            new JsonResponse().error(ErrorType.TOKEN_INVALID).add("additionalInfo", onError)
                                    .apply(ctx);
                            ctx.skipRemainingHandlers();
                        });
                        if (!verified) return;
                        ctx.attribute("token", token);
                        ctx.attribute("username", JWTUtil.instance().getUsername(token));
                    }
                })
                .get("/api/v1/flow/jobs", ctx ->
                        new JsonResponse()
                                .success()
                                .add("jobs", JobAgent.instance().getMinimalJobData())
                                .apply(ctx))
                .get("/api/v1/flow/job/{id}", ctx -> {

                })
                .start(hostAddress, hostPort);
        log.info("Started endpoint on {}:{}", hostAddress, hostPort);
    }

    public void stop() {
        app.stop();
    }


    public static class JsonResponse {
        private final JsonObject object;

        public JsonResponse() {
            object = new JsonObject();
        }
        public JsonResponse success() {
            object.addProperty("success", true);
            return this;
        }
        public JsonResponse error(ErrorType type) {
            object.addProperty("success", false);
            object.addProperty("error", type.code);
            object.addProperty("message", type.message);
            return this;
        }

        public JsonResponse add(String key, String value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonResponse add(String key, Number value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonResponse add(String key, boolean value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonResponse add(String key, char value) {
            object.addProperty(key, value);
            return this;
        }
        public JsonResponse add(String key, JsonElement e) {
            object.add(key, e);
            return this;
        }

        public String toJson() {
            return object.toString();
        }
        public void apply(Context ctx) {
            ctx.result(toJson());
            ctx.contentType("application/json");
            if (object.has("error")) {
                ctx.status(object.get("error").getAsInt());
            }
        }
    }

    public enum ErrorType {
        NO_AUTH(403, "Not authenticated"),
        SERVICE_UNAVAILABLE(503, "Service temporarily unavailable"),
        TOKEN_INVALID(401, "Invalid token"),
        TOKEN_EXPIRED(401, "Token expired"),
        TOKEN_INVALID_ISSUER(401, "Invalid token issuer"),
        TOKEN_INVALID_CLAIMS(401, "Invalid token claims"),
        TOKEN_MISSING(401, "Missing token"),

        ;

        public final int code;
        public final String message;

        ErrorType(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}