package de.julianweinelt.databench.worker.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vdurmont.semver4j.Semver;
import de.julianweinelt.databench.worker.Flow;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
public class UpdateChecker {
    public void startDownload(String version) {
        File toSave = new File("tmp/flow-update.jar");
        if (toSave.getParentFile().mkdirs()) log.debug("Created directory {} for update download.", toSave.getParentFile().getAbsolutePath());
        String url = "https://api.databench.julianweinelt.de/api/v1/download?part=FLOW&version=" + version;
        try {
            new DownloadUtil().downloadFile(url, toSave.getPath());
        } catch (IOException e) {
            log.error("Failed to download update.");
        }
    }

    public void checkForUpdates(boolean giveNegativeFeedback) {
        log.info("Checking for updates...");
        try {
            URL url = new URL("https://api.databench.julianweinelt.de/api/v1/versions");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            JsonObject o = JsonParser.parseString(content.toString()).getAsJsonObject();
            if (o.has("FLOW")) {
                String version = o.get("FLOW").getAsString();
                log.info("Current version is {}.", Flow.version);
                log.info("Latest version is {}.", version);

                Semver current = new Semver(Flow.version);
                Semver server = new Semver(version);
                if (server.isGreaterThan(current)) {
                    log.info("There is a new version available. Please update to {}.", version);

                } else {
                    if (giveNegativeFeedback) {
                        log.info("Flow is up-to-date.");
                    }
                }
            } else {
                log.warn("No version information available.");
            }
            in.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
