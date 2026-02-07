package de.julianweinelt.databench.server.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.databench.server.KeyManager;
import de.julianweinelt.databench.server.VersionManager;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@Slf4j
public class WebServer {
    private Javalin app;

    public void start() {
        app = Javalin.create(cnf -> {
            cnf.showJavalinBanner = false;
            cnf.useVirtualThreads = true;
                })
                .before(ctx -> {
                    ctx.contentType("application/json");
                })
                .get("/api/v1/download", ctx -> {
                    String rawPart = ctx.queryParam("part");
                    String rawVersion = ctx.queryParam("version");
                    if (rawPart == null) {
                        errorResponse(ctx, "Missing parameter part", 400);
                        return;
                    }
                    try {
                        DataBenchPart.valueOf(rawPart.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        errorResponse(ctx, "Invalid parameter part", 400);
                        return;
                    }
                    DataBenchPart part = DataBenchPart.valueOf(rawPart.toUpperCase());
                    if (rawVersion == null) {
                        rawVersion = VersionManager.instance().getLatestVersion(part);
                    }

                    ctx.contentType("application/octet-stream");
                    ctx.header("Content-Disposition", "attachment; filename=\"dataBench-" + part + "-" + rawVersion + ".jar\"");

                    File file = VersionManager.instance().getFile(rawVersion, part);
                    if (!file.exists()) {
                        errorResponse(ctx, "File not found", 404);
                        return;
                    }
                    ctx.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
                    ctx.result(new FileInputStream(file));
                })
                .get("/api/v1/changelog/{version}", ctx -> {
                    String rawPart = ctx.queryParam("part");
                    if (rawPart == null) {
                        errorResponse(ctx, "Missing parameter part", 400);
                        return;
                    }
                    try {
                        DataBenchPart.valueOf(rawPart.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        errorResponse(ctx, "Invalid parameter part", 400);
                        return;
                    }
                    String rawVersion = ctx.pathParam("version");
                    DataBenchPart part = DataBenchPart.valueOf(rawPart.toUpperCase());
                    JsonObject o = new JsonObject();
                    o.addProperty("success", true);
                    o.addProperty("changelog", VersionManager.instance().getChangeLog(rawVersion, part));
                })
                .get("/api/v1/versions", ctx -> {
                    ctx.result(new Gson().toJson(VersionManager.instance().getLatestVersions()));
                })
                .post("/api/v1/latestversion", ctx -> {
                    String key = ctx.header("DataBenchKey");
                    if (!KeyManager.checkKey(key)) {
                        errorResponse(ctx, "Invalid key", 403);
                        return;
                    }
                    JsonObject root = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    try {
                        DataBenchPart part = DataBenchPart.valueOf(root.get("part").getAsString().toUpperCase());
                        String version = root.get("version").getAsString();

                        VersionManager.instance().getLatestVersions().put(part, version);
                        VersionManager.instance().saveLatestVersions();
                        JsonObject o = new JsonObject();
                        o.addProperty("success", true);
                        ctx.result(o.toString());
                    } catch (IllegalArgumentException e) {
                        errorResponse(ctx, "Invalid parameter part", 400);
                    }
                })
                .post("/api/v1/upload", ctx -> {
                    DataBenchPart part = DataBenchPart.valueOf(ctx.queryParam("part").toUpperCase());
                    String version = ctx.queryParam("version");
                    String key = ctx.header("DataBenchKey");
                    if (!KeyManager.checkKey(key)) {
                        ctx.status(403).result("Invalid key");
                        return;
                    }

                    UploadedFile file = ctx.uploadedFile("file");
                    if (file == null) {
                        ctx.status(400).result("No file uploaded");
                        return;
                    }

                    Path target = VersionManager.instance().getFile(version, part).toPath();
                    Files.createDirectories(target.getParent());

                    Files.copy(file.content(), target);
                    log.info("Uploaded file {} to {}", file.filename(), target);

                    JsonObject o = new JsonObject();
                    o.addProperty("success", true);
                    ctx.result(o.toString());
                })
                .post("/api/v1/changelog", ctx -> {
                    DataBenchPart part = DataBenchPart.valueOf(ctx.queryParam("part").toUpperCase());
                    String version = ctx.queryParam("version");
                    String key = ctx.header("DataBenchKey");
                    if (!KeyManager.checkKey(key)) {
                        ctx.status(403).result("Invalid key");
                        return;
                    }

                    String changelog = ctx.body();
                    try (FileWriter w = new FileWriter(VersionManager.instance().getChangelogFile(version, part))) {
                        w.write(changelog);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);

                        JsonObject o = new JsonObject();
                        o.addProperty("success", false);
                        ctx.result(o.toString());
                        return;
                    }


                    JsonObject o = new JsonObject();
                    o.addProperty("success", true);
                    ctx.result(o.toString());
                })
                .post("/metrics/login", ctx -> {

                })
                .start(7000);
    }

    public void stop() {
        app.stop();
    }

    private void errorResponse(Context ctx, String message, int code) {
        JsonObject o = new JsonObject();
        o.addProperty("message", message);
        o.addProperty("code", code);
        o.addProperty("success", false);
        ctx.status(code).result(o.toString());
    }
}