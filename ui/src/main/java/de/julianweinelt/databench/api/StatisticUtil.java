package de.julianweinelt.databench.api;

import com.google.gson.JsonObject;
import de.julianweinelt.databench.DataBench;
import de.julianweinelt.databench.data.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
public class StatisticUtil {
    public static void sendStartup() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("version", DataBench.version);
            o.addProperty("os", System.getProperty("os.name"));
            o.addProperty("java", System.getProperty("java.version"));
            o.addProperty("cpu", Runtime.getRuntime().availableProcessors());
            o.addProperty("ram", Runtime.getRuntime().maxMemory());
            o.addProperty("installationID", Configuration.getConfiguration().getInstallationID().toString());

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.databench.julianweinelt.de/metrics/login"))
                    .POST(HttpRequest.BodyPublishers.ofString(o.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Failed to send anonymous statistics"
                                + response.statusCode() + " -> " + response.body());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}