package de.julianweinelt.datacat.server.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class LocalStorage {
    private final File configFile = new File("config.json");

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    @Getter
    private Configuration config = new Configuration();

    private static LocalStorage instance;
    public static LocalStorage instance() {
        return instance;
    }

    public LocalStorage() {
        instance = this;

        if (!configFile.exists()) save();

        load();
    }

    public void save() {
        try (FileWriter w = new FileWriter(configFile)) {
            w.write(GSON.toJson(config));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
    public void load() {
        try (FileReader r = new FileReader(configFile)) {
            config = GSON.fromJson(r, Configuration.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
