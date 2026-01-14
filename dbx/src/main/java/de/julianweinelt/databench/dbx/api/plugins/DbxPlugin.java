package de.julianweinelt.databench.dbx.api.plugins;

import de.julianweinelt.databench.dbx.api.DbxAPI;
import de.julianweinelt.databench.dbx.api.Registry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
    private String[] authors;
    private String version;

    private Path jarURL;
    private String minAPIVersion;
    private boolean storesSensitiveData = false;
    private boolean usesEncryption = false;
    private final List<String> dependencies = new ArrayList<>();
    private final List<String> optionalDependencies = new ArrayList<>();

    private final HashMap<String, HashMap<String, String>> languageData = new HashMap<>();

    public void setLangData(HashMap<String, HashMap<String, String>> languageData) {
        if (languageData.isEmpty()) log.warn("Language data is empty.");
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
}