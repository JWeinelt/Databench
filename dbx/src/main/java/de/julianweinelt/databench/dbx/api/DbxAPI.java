package de.julianweinelt.databench.dbx.api;

import de.julianweinelt.databench.dbx.util.DatabaseType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DbxAPI {
    private final File apiFolder;
    private static DbxAPI instance;

    private final List<DatabaseType> types = new ArrayList<>();

    @Getter
    private final Registry registry;

    public DbxAPI(File apiFolder) {
        instance = this;
        this.apiFolder = apiFolder;
        if (apiFolder.mkdirs()) log.debug("API folder created");
        registry = new Registry(this);
        init();
    }

    public static DbxAPI instance() {
        return instance;
    }

    private void init() {
        if (new File(apiFolder, "plugins").mkdirs()) log.debug("API plugins folder created");
        if (new File(apiFolder, "drivers").mkdirs()) log.debug("API drivers folder created");
        types.add(new DatabaseType("MySQL", "", "", "Oracle"));
    }

    // API Methods
    public void registerDatabaseType(String name, String jdbcLink, String driverClass, String vendor) {
        types.add(new DatabaseType(name, jdbcLink, driverClass, vendor));
    }
    public boolean typeRegistered(String driverClass) {
        for (DatabaseType type : types) {
            if (type.driverClass().equals(driverClass)) return true;
        }
        return false;
    }

    // Getters
    public static Registry registry() {
        return instance().registry;
    }
    public static File pluginsFolder() {
        return new File(instance.apiFolder, "plugins");
    }
    public static File driversFolder() {
        return new File(instance.apiFolder, "drivers");
    }
}