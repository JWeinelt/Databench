package de.julianweinelt.databench.flow.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class LocalStorage {
    private final File configFile;
    private static LocalStorage instance;
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    @Getter
    private Configuration config = new Configuration();

    public static LocalStorage instance() {
        return instance;
    }

    public LocalStorage(File configFile) {
        this.configFile = configFile;
        instance = this;
    }

    public boolean configCreated() {
        return configFile.exists();
    }

    public void save() {
        try (FileWriter w = new FileWriter(configFile)) {
            w.write(GSON.toJson(config));
        } catch (IOException e) {
            log.error("Failed to save configuration file.");
            log.error(e.getMessage());
        }
    }

    public void load() {
        if (!configCreated()) {
            save();
            log.info("Configuration file has been created.");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            config = GSON.fromJson(br, Configuration.class);
        } catch (IOException e) {
            log.error("Failed to load configuration file.");
            log.error(e.getMessage());
        }
    }
}
