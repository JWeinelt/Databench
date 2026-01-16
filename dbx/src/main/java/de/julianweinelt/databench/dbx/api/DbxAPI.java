package de.julianweinelt.databench.dbx.api;

import de.julianweinelt.databench.dbx.api.ui.UIService;
import de.julianweinelt.databench.dbx.database.DatabaseRegistry;
import de.julianweinelt.databench.dbx.database.providers.DBMetaMSSQL;
import de.julianweinelt.databench.dbx.database.providers.DBMetaMariaDB;
import de.julianweinelt.databench.dbx.database.providers.DBMetaMySQL;
import de.julianweinelt.databench.dbx.database.providers.db.DBMSSQL;
import de.julianweinelt.databench.dbx.database.providers.db.DBMariaDB;
import de.julianweinelt.databench.dbx.database.providers.db.DBMySQL;
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
    @Getter
    private UIService uiService;
    @Getter
    private DatabaseRegistry dbRegistry;

    public DbxAPI(File apiFolder) {
        instance = this;
        this.apiFolder = apiFolder;
        if (apiFolder.mkdirs()) log.debug("API folder created");
        registry = new Registry(this);
        dbRegistry = new DatabaseRegistry();
        uiService = new UIService();
        init();
    }

    public static DbxAPI instance() {
        return instance;
    }

    private void init() {
        if (new File(apiFolder, "plugins").mkdirs()) log.debug("API plugins folder created");
        if (new File(apiFolder, "drivers").mkdirs()) log.debug("API drivers folder created");
        types.add(new DatabaseType("MySQL", "jdbc:mysql://${server}/${database}" +
                "?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=" +
                "UTC&autoReconnect=true", "Oracle"));


        log.info("Registering database handlers...");
        DatabaseRegistry.instance().registerMapping("mysql", DBMySQL::new, new DBMetaMySQL());
        DatabaseRegistry.instance().registerMapping("mssql", DBMSSQL::new, new DBMetaMSSQL());
        DatabaseRegistry.instance().registerMapping("mariadb", DBMariaDB::new, new DBMetaMariaDB());
    }

    // API Methods
    public void registerDatabaseType(String name, String jdbcLink, String vendor) {
        types.add(new DatabaseType(name, jdbcLink, vendor));
    }
    public boolean typeRegistered(String name) {
        for (DatabaseType type : types) {
            if (type.name().equals(name)) return true;
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