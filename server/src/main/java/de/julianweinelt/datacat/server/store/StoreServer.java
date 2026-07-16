package de.julianweinelt.datacat.server.store;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.password4j.HashChecker;
import com.password4j.Password;
import de.julianweinelt.datacat.server.store.account.Account;
import de.julianweinelt.datacat.server.store.account.AccountCreationResponse;
import de.julianweinelt.datacat.server.store.account.AccountLinkType;
import de.julianweinelt.datacat.server.store.account.AccountManager;
import de.julianweinelt.datacat.server.store.database.DBStorage;
import de.julianweinelt.datacat.server.store.yarn.Yarn;
import de.julianweinelt.datacat.server.util.JWTUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;
import io.javalin.http.staticfiles.Location;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
public class StoreServer {
    @Setter
    private boolean maintenance = false;
    private Javalin app;

    public StoreServer() {
        File mainStore = new File("store");
        if (mainStore.mkdirs()) {
            File pluginFolder = new File(mainStore, "plugins");
            if (pluginFolder.mkdirs()) {
                new File(pluginFolder, "images").mkdirs();
            }
        }
    }

    public void start() {
        app = Javalin.create(conf -> {
            conf.showJavalinBanner = false;
            conf.staticFiles.add("public-store", Location.EXTERNAL);
        }).error(404, ctx -> {
            if (!ctx.path().contains("api"))
                ctx.html(Files.readString(new File("public-store/error/404.html").toPath()));
            else error(ErrorResponseType.ROUTE_NOT_FOUND, ctx).status(404);
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
                    if (path.startsWith("/api")) return;

                    ctx.status(503);
                    ctx.contentType("text/html");
                    ctx.result(Files.readString(new File("public-store/maintenance/index.html").toPath()));
                    ctx.skipRemainingHandlers();
                })

                .post("/api/v1/auth/login/username", ctx -> {
                    String header = ctx.header("Authorization");
                    if (header == null) {
                        error(ErrorResponseType.INVALID_REQUEST, ctx);
                        return;
                    }

                    byte[] data = Base64.getDecoder().decode(header.replace("Basic ", ""));
                    String decoded = new String(data);
                    String[] split = decoded.split(":");
                    if (split.length != 2) {
                        error(ErrorResponseType.INVALID_REQUEST, ctx);
                    }
                    String username = split[0];
                    String password = split[1];
                    Account account = AccountManager.instance().getAccount(username, DBStorage.LoadMethod.USERNAME);
                    if (account == null) {
                        error(ErrorResponseType.USER_NOT_FOUND, ctx);
                        return;
                    }
                    HashChecker passwordMatch = Password.check(password, account.getPasswordHash());
                    boolean passwordCorrect = passwordMatch.withArgon2();

                    if (!passwordCorrect) {
                        error(ErrorResponseType.INVALID_PASSWORD, ctx);
                        return;
                    }
                    JsonObject o = new JsonObject();
                    o.addProperty("success", true);
                    o.addProperty("token", JWTUtil.instance().generateToken(account.getUniqueId()));

                    ctx.result(o.toString());
                })
                .post("/api/v1/auth/register", ctx -> {
                    String bodyStr = ctx.body();
                    JsonObject body = JsonParser.parseString(bodyStr).getAsJsonObject();

                    String username = body.get("username").getAsString();
                    String password = new String(Base64.getDecoder().decode(body.get("password").getAsString()));
                    String email = body.get("email").getAsString();

                    AccountCreationResponse response = AccountManager.instance().createAccount(username, email, password);
                    if (response.equals(AccountCreationResponse.CREATED)) {
                        ctx.status(HttpStatus.CREATED);
                        return;
                    } else if (response.equals(AccountCreationResponse.USERNAME_ALREADY_EXISTS)) {
                        error(ErrorResponseType.USER_ALREADY_EXISTS, ctx);
                    } else if (response.equals(AccountCreationResponse.EMAIL_ALREADY_EXISTS)) {
                        error(ErrorResponseType.EMAIL_ALREADY_EXISTS, ctx);
                    }
                })

                .get("/api/v1/profile/minimal", ctx -> {
                    Account account = extractAccount(ctx);
                    if (account == null) {
                        error(ErrorResponseType.USER_NOT_FOUND, ctx);
                        return;
                    }

                    JsonObject o = new JsonObject();
                    o.addProperty("username", account.getUsername());
                    o.addProperty("email", account.getEmail());
                    o.addProperty("uuid", account.getUniqueId().toString());
                    ctx.result(o.toString());
                })
                .get("/api/v1/profile/public/{username}", ctx -> {
                    ctx.contentType("application/json");
                    String username = ctx.pathParam("username");
                    Account account;
                    if (username.equalsIgnoreCase("me")) {
                        account = extractAccount(ctx);
                    } else {
                        if (username.startsWith("@")) username = username.substring(1);
                        account = AccountManager.instance().getAccount(username, DBStorage.LoadMethod.USERNAME);
                    }

                    if (account == null) {
                        error(ErrorResponseType.USER_NOT_FOUND, ctx);
                        return;
                    }

                    JsonObject o = new JsonObject();
                    o.addProperty("username", account.getUsername());
                    o.addProperty("id", account.getUniqueId().toString());
                    o.addProperty("created", account.getCreationTime());
                    o.addProperty("verifyStatus", account.getVerificationStatus().name());
                    JsonObject yarnA = new JsonObject();
                    JsonArray yarns = new JsonArray();
                    yarnA.add("yarns", yarns);
                    JsonArray pinnedYarns = new JsonArray();
                    yarnA.add("pinned", pinnedYarns);
                    o.add("yarns", yarnA);

                    o.add("links", AccountManager.instance().getAccountLinkJS(account));

                    o.addProperty("yourself", ctx.header("Authorization") != null && account.getUniqueId().equals(extractAccount(ctx).getUniqueId()));

                    ctx.result(o.toString());
                })
                .post("/api/v1/profile/upload", ctx -> {
                    Account account = extractAccount(ctx);
                    if (account == null) {
                        error(ErrorResponseType.LOGIN_REQUIRED, ctx);
                        return;
                    }

                    UploadedFile file = ctx.uploadedFile("image");
                    if (file == null) {
                        error(ErrorResponseType.INVALID_FILE, ctx);
                        return;
                    }

                    String contentType = file.contentType();

                    if (contentType == null || !contentType.startsWith("image/")) {
                        error(ErrorResponseType.INVALID_FILE, ctx);
                        return;
                    }

                    String fileName = account.getUniqueId() + ".png";
                    File saveDir = new File("store", "profiles");
                    if (saveDir.mkdirs()) log.debug("Created profiles folder");

                    Path profilePath = new File(saveDir, fileName).toPath();

                    try (InputStream in = file.content()) {
                        Files.copy(in, profilePath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    ctx.result(fileName);
                })
                .get("/api/v1/assets/profile/{userid}", ctx -> {
                    String userid = ctx.pathParam("userid");
                    File file = new File("store/profiles", userid + ".png");
                    if (file.exists()) {
                        ctx.contentType("image/png");
                        ctx.result(Files.newInputStream(file.toPath()));
                    } else {
                        File placeholder = new File("store/profiles", "__placeholder.png");
                        ctx.contentType("image/png");
                        ctx.result(Files.newInputStream(placeholder.toPath()));
                    }
                })

                .patch("/api/v1/profile/edit/link", ctx -> {
                    JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    Account account = extractAccount(ctx);
                    if (account == null) {
                        error(ErrorResponseType.LOGIN_REQUIRED, ctx);
                        return;
                    }
                    String type = body.get("type").getAsString();
                    String data = body.get("link").getAsString();
                    AccountLinkType t = AccountLinkType.get(type);
                    if (t == null) {
                        error(ErrorResponseType.INVALID_REQUEST, ctx);
                        return;
                    }

                    AccountManager.instance().updateAccountLink(account, t, data);
                    ctx.status(201);
                })
                .post("/api/v1/profile/upload-picture", ctx -> {
                    Account account = extractAccount(ctx);
                    if (account == null) {
                        error(ErrorResponseType.LOGIN_REQUIRED, ctx);
                        return;
                    }

                    UploadedFile file = ctx.uploadedFile("image");
                    if (file == null) {
                        error(ErrorResponseType.INVALID_FILE, ctx);
                        return;
                    }

                    Path savePath = new File("store/profiles", account.getUniqueId() + ".png").toPath();

                    try (InputStream in = file.content()) {
                        Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    ctx.status(200);
                })
                .post("/api/v1/yarn/create", ctx -> {
                    Account account = extractAccount(ctx);
                    if (account == null) {
                        error(ErrorResponseType.LOGIN_REQUIRED, HttpStatus.UNAUTHORIZED, ctx);
                        return;
                    }

                    try {
                        JsonParser.parseString(ctx.body()).getAsJsonObject();
                    } catch (Exception e) {
                        error(ErrorResponseType.INVALID_REQUEST, ctx);
                        return;
                    }

                    JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    DBStorage.instance().createYarnMetaData(body, account.getUniqueId()).thenAccept(uuid -> {
                        ctx.status(201);
                        JsonObject o = new JsonObject();
                        o.addProperty("success", true);
                        o.addProperty("id", uuid.toString());
                        ctx.result(o.toString());
                    }).exceptionally(ex -> {
                        error(ErrorResponseType.INVALID_REQUEST, ctx);
                        return null;
                    });
                })

                .get("/api/v1/yarn/id/{id}", ctx -> {
                    UUID id = UUID.fromString(ctx.pathParam("id"));
                    Yarn yarn = YarnManager.getYarn(id);
                    if (yarn == null) {
                        error(ErrorResponseType.YARN_NOT_FOUND, ctx);
                        return;
                    }
                    ctx.result(new Gson().toJson(yarn));
                })

                .get("/api/v1/assets/plugins/{id}/image", ctx -> {
                    String id = ctx.pathParam("id");
                    File file = new File("store/plugins/images/" + id + ".png");
                    if (file.exists()) {
                        ctx.contentType("image/png");
                        ctx.result(Files.newInputStream(file.toPath()));
                    } else {
                        ctx.status(404);
                    }
                })
                .get("/api/v1/yarn/popular", ctx -> {

                })
                .get("/yarn/{slug}", ctx -> {
                    ctx.contentType("text/html");
                    ctx.result(Files.readString(new File("public-store/view/index.html").toPath()));
                })
                .get("/user/@{username}", ctx -> {
                    ctx.contentType("text/html");
                    ctx.result(Files.readString(new File("public-store/account/index.html").toPath()));
                })
                .get("/user/{something}", ctx -> {
                    String username = ctx.pathParam("something");
                    if (username.startsWith("@")) return;
                    if (username.endsWith(".js")) return;
                    else ctx.redirect("/user/" + "@" + username);
                })
        .start(7001);
    }

    public Context error(ErrorResponseType type, Context ctx) {
        JsonObject o = new JsonObject();
        o.addProperty("success", false);
        o.addProperty("type", type.name());
        ctx.status(400).result(o.toString());
        return ctx;
    }
    public Context error(ErrorResponseType type, HttpStatus code, Context ctx) {
        JsonObject o = new JsonObject();
        o.addProperty("success", false);
        o.addProperty("type", type.name());
        ctx.status(code).result(o.toString());
        return ctx;
    }

    private Account extractAccount(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null) return null;
        String token = header.replace("Bearer ", "");
        DecodedJWT decodedJWT = JWTUtil.instance().decode(token);
        if (decodedJWT == null) return null;
        return AccountManager.instance().getAccount(decodedJWT.getSubject(), DBStorage.LoadMethod.UUID);
    }

    public void stop() {
        app.stop();
    }

    public enum ErrorResponseType {
        USER_NOT_FOUND,
        INVALID_PASSWORD,
        INVALID_REQUEST,

        USER_ALREADY_EXISTS,
        EMAIL_ALREADY_EXISTS,

        ROUTE_NOT_FOUND,

        YARN_NOT_FOUND,

        INVALID_FILE,
        LOGIN_REQUIRED
    }
}