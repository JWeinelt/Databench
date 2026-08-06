package de.julianweinelt.datacat.dbx.api.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.julianweinelt.datacat.dbx.api.DbxAPI;
import de.julianweinelt.datacat.dbx.api.Registry;
import de.julianweinelt.datacat.dbx.api.ui.theme.Theme;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

    @Getter @Setter
    private boolean enabled = false;

    private final HashMap<String, HashMap<String, String>> languageData = new HashMap<>();

    protected void setLangData(HashMap<String, HashMap<String, String>> languageData) {
        if (languageData.isEmpty()) log.debug("Language data is empty.");
        this.languageData.clear();
        this.languageData.putAll(languageData);
    }

    /**
     * Get the data folder of the plugin. Typically, the path is ~/data/[ModuleName].
     * @return {@link File} object of the data folder
     */
    protected File getDataFolder() {
        return new File(DbxAPI.pluginsFolder(), "data/" + name);
    }

    /**
     * Get the registry of the system
     * @return A {@link Registry} object representing the API registry
     */
    protected Registry getRegistry() {
        return Registry.instance();
    }

    /**
     * Get the main frame of the editor window
     * @return The {@link JFrame} object of the editor window
     */
    protected JFrame getMainFrame() {
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

    /**
     * Get the logger implementing class to log data to console/debug protocol
     * @return A {@link Logger} instance
     */
    protected Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Defines if this plugin is required on both client (editor) and server (flow).<br>
     * Example how it is handled:<br>
     * <ul>
     *     <li>Server: <code>true</code> + Client: <code>true</code> => Plugin can be used</li>
     *     <li>Server: <code>false</code> + Client: <code>true</code> => Plugin cannot be used</li>
     *     <li>Server: <code>true</code> + Client: <code>false</code> => Client will be forced to download plugin</li>
     * </ul>
     * @return
     */
    public boolean requiredOnClientAndServer() {
        return false;
    }

    /**
     * Register a theme that is located in the classpath in resources folder.
     * @param name Name of theme, the file <b>must</b> have the same name under
     *             <code>src/main/resources/[name].theme.json</code>
     * @deprecated In favor of the new yarn theme system since v1.0.1-beta.1
     */
    protected void registerTheme(String name) {
        String filePath = "/themes/" + name + ".theme.json";
        try {
            String data = readContent(filePath);
            Theme theme = new Theme(this, name, data);
            getRegistry().registerTheme(theme);
        } catch (IOException e) {
            log.error("Failed to load theme data for theme {}, plugin {} by {}. File not found at {}",
                    name, getName(), String.join(",", getAuthors()), filePath);
        }
    }

    /**
     * Register a theme using an internal name and a {@link BasicLookAndFeel} instance
     * @param name The internal name of the theme. Should only be lower case letters and underscores.
     * @param laf An instance of {@link BasicLookAndFeel}
     * @deprecated In favor of the new yarn theme system since v1.0.1-beta.1
     */
    protected void registerTheme(String name, BasicLookAndFeel laf) {
        Theme theme = new Theme(this, name, laf);
        getRegistry().registerTheme(theme);
    }

    /**
     * Loads themes from classpath, reading the content of <code>src/main/resources/themes/themes.json</code>
     */
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