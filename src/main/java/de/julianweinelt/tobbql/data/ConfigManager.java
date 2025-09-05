package de.julianweinelt.tobbql.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.tobbql.TobbeBench;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Type;

@Slf4j
public class ConfigManager {
    private final File configFile = new File("config.json");

    @Getter
    private Configuration configuration = new Configuration();

    public static ConfigManager getInstance() {
        return TobbeBench.getInstance().getConfigManager();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            saveConfig();
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line;
            StringBuilder jsonStringBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) jsonStringBuilder.append(line);

            Type type = new TypeToken<Configuration>(){}.getType();
            configuration = new Gson().fromJson(jsonStringBuilder.toString(), type);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void saveConfig() {
        try (FileWriter w = new FileWriter(configFile)) {
            w.write(new GsonBuilder().setPrettyPrinting().create().toJson(configuration));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
