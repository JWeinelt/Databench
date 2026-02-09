package de.julianweinelt.databench.ui.flow;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.databench.api.DConnection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class FlowClient {
    private String baseURL = "http://localhost:47387/api/v1/";
    @Getter
    private String token;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final DConnection connection;
    @Getter
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    private static FlowClient instance;

    public static FlowClient instance() {
        return instance;
    }

    public FlowClient(DConnection connection) {
        log.info("Initializing FlowClient for project {}", connection.getProject().getName());
        this.connection = connection;
        instance = this;
        log.info("Checking server availability");
        checkServerThere();
    }

    private void checkServerThere() {
        String host = connection.getProject().getServer();
        if (host.split(":").length == 2) host = host.split(":")[0];
        baseURL = "http://" + host + ":47387/api/v1/";
        get("hello", new HeaderBuilder())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        log.warn("Got status code {} from server.", response.statusCode());
                        enabled.set(false);
                        return;
                    }
                    JsonObject data = response.body();
                    if (data.has("flowVersion")) {
                        enabled.set(true);
                        log.info("Flow Server detected.");
                    }
                }).exceptionally(ex -> {
                    log.warn("Could not connect to Flow Server.", ex);
                    enabled.set(false);
                    return null;
                });
    }

    // Helper Methods
    private HttpResponse<JsonObject> send(HttpRequest request) {
        try {
            java.net.http.HttpResponse<String> response =
                    httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            JsonObject body = null;
            String raw = response.body();

            if (raw != null && !raw.isBlank()) {
                body = JsonParser.parseString(raw).getAsJsonObject();
            } else {
                log.warn("Server responded with empty body.");
            }

            return new HttpResponse<>(
                    response.statusCode(),
                    response.headers().map(),
                    body
            );

        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }


    public CompletableFuture<Boolean> login(String username, String password, Consumer<String> onError) {
        String base = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        return post("auth",
                new HeaderBuilder().add("Authorization", "Basic " + base),
                null
        ).thenApply(response -> {
            if (response.statusCode() == 200 &&
                    response.body() != null &&
                    response.body().get("success").getAsBoolean()) {
                enabled.set(true);
                token = response.body().get("token").getAsString();
                log.info("Successfully logged in.");
                return true;
            }

            if (response.body() != null) {
                log.warn("Login failed: {}", response.body().get("message").getAsString());
                onError.accept(response.body().get("message").getAsString());
            } else {
                log.warn("Login failed with code {}", response.statusCode());
                onError.accept("Unknown error");
            }

            return false;
        });
    }

    public CompletableFuture<HttpResponse<JsonObject>> get(String path, HeaderBuilder header) {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + path))
                .GET()
                .timeout(Duration.ofSeconds(10));

        header.apply(builder);

        return CompletableFuture.supplyAsync(() -> send(builder.build()));
    }

    public CompletableFuture<HttpResponse<JsonObject>> post(String path, HeaderBuilder header, String body) {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + path))
                .timeout(Duration.ofSeconds(10));

        if (body == null || body.isBlank()) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
        }

        header.apply(builder);

        return CompletableFuture.supplyAsync(() -> send(builder.build()));
    }


    public static class HeaderBuilder {
        private final HashMap<String, String> headers = new HashMap<>();

        public HeaderBuilder add(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public void apply(HttpRequest.Builder builder) {
            headers.forEach(builder::header);
        }

        public static HeaderBuilder createDefault() {
            return new HeaderBuilder()
                    .add("Authorization", "Bearer " + FlowClient.instance().getToken());
        }
    }
}