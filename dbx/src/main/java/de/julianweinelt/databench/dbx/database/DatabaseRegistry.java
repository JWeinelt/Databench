package de.julianweinelt.databench.dbx.database;

import java.util.HashMap;
import java.util.Optional;

public class DatabaseRegistry {
    private final HashMap<String, ADatabase> databases = new HashMap<>();

    public static DatabaseRegistry instance;
    public static DatabaseRegistry instance() {
        return instance;
    }
    public DatabaseRegistry() {
        instance = this;
    }

    public void addDatabase(String name, ADatabase database) {
        databases.put(name.toUpperCase(), database);
    }
    public Optional<ADatabase> getDatabase(String name) {
        return Optional.ofNullable(databases.getOrDefault(name.toUpperCase(), null));
    }
}
