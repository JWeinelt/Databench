package de.julianweinelt.databench.dbx.database;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class DatabaseRegistry {
    private final Map<String, DatabaseFactory> databases = new HashMap<>();
    private final Map<String, DatabaseMetaData> metaData = new HashMap<>();

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

    public void registerMapping(String name, DatabaseFactory factory, DatabaseMetaData meta) {
        databases.put(name.toUpperCase(), factory);
        metaData.put(name.toUpperCase(), meta);
        log.info("Registered mapping for {}", name);
    }

    public DatabaseMetaData getMeta(String name) {
        return metaData.getOrDefault(name.toUpperCase(), null);
    }
    public List<DatabaseMetaData> getDatabaseTypes() {
        return new ArrayList<>(metaData.values());
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