package de.julianweinelt.datacat.dbx.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
public class NewsFetcher {
    private static final String BASE_URL = "https://api.data-cat.de/api/v1/news/latest/";

    public static News fetch() {
        String text = getText();
        JsonObject meta = getMeta();
        return new News(meta.get("title").getAsString(), text, meta.get("date").getAsLong());
    }

    private static String getText() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "html"))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            log.error("Failed to fetch news", e);
            return "";
        }
    }

    private static JsonObject getMeta() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "meta"))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            log.info("News meta: {}", body);
            if (!body.isEmpty())
                return JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            log.error("Failed to fetch news meta", e);
        }
        JsonObject o = new JsonObject();
        o.addProperty("title", "Not found");
        o.addProperty("date", 0);
        return o;
    }

    public record News(String title, String text, long date) {}
}
