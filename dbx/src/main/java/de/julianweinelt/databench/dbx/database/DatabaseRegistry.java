package de.julianweinelt.databench.dbx.database;

import java.util.HashMap;
import java.util.Map;

public class DatabaseRegistry {
    private final Map<String, DatabaseFactory> databases = new HashMap<>();

    @FunctionalInterface
    public interface DatabaseFactory {
        ADatabase create(String host, int port, String username, String password);
    }

    private static DatabaseRegistry instance;

    public static DatabaseRegistry instance() {
        return instance;
    }

    public DatabaseRegistry() {
        instance = this;
    }

    public void registerMapping(String name, DatabaseFactory factory) {
        databases.put(name.toUpperCase(), factory);
    }

    public ADatabase instantiate(
            String type,
            String host,
            int port,
            String username,
            String password
    ) {
        DatabaseFactory factory = databases.get(type.toUpperCase());
        if (factory == null) {
            throw new IllegalArgumentException("Unknown database type: " + type);
        }
        return factory.create(host, port, username, password);
    }
}