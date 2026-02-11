package de.julianweinelt.databench.dbx.api.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.julianweinelt.databench.dbx.api.DbxAPI;
import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.ui.theme.Theme;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Getter
@Setter
public abstract class DbxPlugin {
    private String name;
    private String description;
    private List<String> authors;
    private String version;

    private Path jarURL;
    private String minAPIVersion;
    private boolean storesSensitiveData = false;
    private boolean usesEncryption = false;
    private final List<String> dependencies = new ArrayList<>();
    private final List<String> optionalDependencies = new ArrayList<>();

    private final HashMap<String, HashMap<String, String>> languageData = new HashMap<>();

    public void setLangData(HashMap<String, HashMap<String, String>> languageData) {
        if (languageData.isEmpty()) log.debug("Language data is empty.");
        this.languageData.clear();
        this.languageData.putAll(languageData);
    }

    /**
     * Returns the data folder of the plugin. Typically, the path is ~/data/[ModuleName].
     * @return {@link File} object of the data folder
     */
    public File getDataFolder() {
        return new File(DbxAPI.pluginsFolder(), "data/" + name);
    }
    public Registry getRegistry() {
        return Registry.instance();
    }
    public JFrame getMainFrame() {
        return Registry.instance().getMainFrame();
    }

    /**
     * Called when plugin is loaded. API calls should not be done here, as dependencies may not be loaded at this time.
     */
    public abstract void preInit();

    /**
     * Called when the plugin is enabled. All dependencies are loaded.
     */
    public abstract void init();

    /**
     * Called when plugin is being disabled
     */
    public abstract void onDisable();

    /**
     * Called to define events in {@link Registry}.
     */
    public abstract void onDefineEvents();

    protected Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    public boolean requiredOnClientAndServer() {
        return false;
    }



    protected void registerTheme(String name) {
        String filePath = "/themes/" + name + ".theme.json";
        try {
            String data = readContent(filePath);
            Theme theme = new Theme(this, name, data);
        } catch (IOException e) {
            log.error("Failed to load theme data for theme {}, plugin {} by {}. File not found at {}",
                    name, getName(), String.join(",", getAuthors()), filePath);
        }
    }

    protected void preloadThemes() {
        try {
            JsonArray definedThemes = JsonParser.parseString(readContent("/themes/themes.json")).getAsJsonArray();
            log.info("The plugin {} defines {} theme(s) using the classpath. Loading...", getName(), definedThemes.size());
            for (JsonElement e : definedThemes) {
                if (e.isJsonObject()) {
                    String theme = e.getAsString();
                    registerTheme(theme);
                }
            }
        } catch (IOException e) {
            log.info("Plugin {} does not predefine themes via classpath. Not loading any themes automatically.", getName());
        }
    }

    private String readContent(String filePath) throws IOException {
        try (InputStream iS = getClass().getResourceAsStream(filePath)) {
            if (iS == null) throw new FileNotFoundException();

            return new String(iS.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}