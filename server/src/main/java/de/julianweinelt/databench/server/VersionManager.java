package de.julianweinelt.databench.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.databench.server.server.DataBenchPart;
import de.julianweinelt.databench.server.server.Version;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class VersionManager {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final File versionsFile = new File("versions.json");
    private final File latestVersionFile = new File("latest.json");
    private final List<Version> versions = new ArrayList<>();
    @Getter
    private final HashMap<DataBenchPart, String> latestVersions = new HashMap<>();

    public static VersionManager instance() {
        return Server.getInstance().getVersionManager();
    }

    public String getLatestVersion(DataBenchPart part) {
        return latestVersions.getOrDefault(part, "!");
    }

    public File getFile(String version, DataBenchPart part) {
        File versionFolder = new File(new File("files", part.folder), version);
        return new File(versionFolder, part.folder + ".jar");
    }

    public File getChangelogFile(String version, DataBenchPart part) {
        File versionFolder = new File(new File("files", part.folder), version);
        return new File(versionFolder, "changelog.md");
    }
    public String getChangeLog(String version, DataBenchPart part) {
        File versionFolder = new File(new File("files", part.folder), version);
        File file = new File(versionFolder, "changelog.md");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } catch (IOException e) {
            return "No changelog available.";
        }
    }

    public void save() {
        try (FileWriter w = new FileWriter(versionsFile)) {
            w.write(GSON.toJson(versions));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void load() {
        if (!versionsFile.exists()) {
            save();
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(versionsFile))) {
            Type type = new TypeToken<List<Version>>(){}.getType();
            versions.addAll(GSON.fromJson(br, type));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void saveLatestVersions() {
        try (FileWriter w = new FileWriter(latestVersionFile)) {
            w.write(GSON.toJson(latestVersions));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void loadLatestVersions() {
        if (!latestVersionFile.exists()) {
            saveLatestVersions();
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(latestVersionFile))) {
            Type type = new TypeToken<HashMap<DataBenchPart, String>>(){}.getType();
            latestVersions.putAll(GSON.fromJson(br, type));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
