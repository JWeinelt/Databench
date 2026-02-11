package de.julianweinelt.databench.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vdurmont.semver4j.Semver;
import de.julianweinelt.databench.DataBench;
import de.julianweinelt.databench.ui.BenchUI;
import de.julianweinelt.databench.ui.DownloadDialogProgress;
import de.julianweinelt.databench.ui.NotificationPopup;
import de.julianweinelt.databench.ui.NotificationType;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import static de.julianweinelt.databench.dbx.util.LanguageManager.translate;

@Slf4j
public class UpdateChecker {
    private final BenchUI ui;

    public UpdateChecker(BenchUI ui) {
        this.ui = ui;
    }

    public static UpdateChecker instance() {
        return DataBench.getInstance().getUpdateChecker();
    }

    public void openDownloadPopup(String version) {
        File toSave = new File("tmp/dbench-update.jar");
        if (toSave.getParentFile().mkdirs()) log.debug("Created directory {} for update download.", toSave.getParentFile().getAbsolutePath());
        String url = "https://api.databench.julianweinelt.de/api/v1/download?part=EDITOR&version=" + version;
        DownloadDialogProgress dialog = new DownloadDialogProgress(ui.getFrame(), url, toSave, "update", () ->
                new NotificationPopup(
                ui.getFrame(),
                ui.getFrame(),
                NotificationType.INFO,
                translate("notification.update-ready.title"),
                translate("notification.update-ready.text"),
                translate("notification.update-ready.link"),
                () -> {
                    DataBench.shouldUpdate = true;
                    System.exit(0);
                }
        ).showPopup());
        dialog.setVisible(true);
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
            if (o.has("EDITOR")) {
                String version = o.get("EDITOR").getAsString();
                log.info("Current version is {}.", DataBench.version);
                log.info("Latest version is {}.", version);

                Semver current = new Semver(DataBench.version);
                Semver server = new Semver(version);
                if (server.isGreaterThan(current)) {
                    log.info("There is a new version available. Please update to {}.", version);
                    new NotificationPopup(
                            ui.getFrame(),
                            ui.getFrame(),
                            NotificationType.INFO,
                            translate("notification.update-available.title"),
                            translate("notification.update-available.text"),
                            translate("notification.update-available.link"),
                            () -> openDownloadPopup(version)
                    ).showPopup();
                } else {
                    if (giveNegativeFeedback) {
                        new NotificationPopup(
                                ui.getFrame(),
                                ui.getFrame(),
                                NotificationType.INFO,
                                translate("notification.no-update.title"),
                                translate("notification.no-update.text"),
                                translate("notification.no-update.link"),
                                () -> {

                                }
                        ).showPopup();
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
