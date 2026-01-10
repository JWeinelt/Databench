package de.julianweinelt.databench.launcher.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class LocalStorage {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    @Getter
    private final File configFile = new File("launcher.config");
    @Getter
    private Configuration configuration = new Configuration();

    public void load() {
        if (!configFile.exists()) {
            save();
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            configuration = GSON.fromJson(br, Configuration.class);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void save() {
        try (FileWriter w = new FileWriter(configFile)) {
            w.write(GSON.toJson(configuration));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
