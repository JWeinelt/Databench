package de.julianweinelt.databench.dbx.util;

import de.julianweinelt.databench.dbx.api.DbxAPI;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class HomeDirectories {
    private final HashMap<String, File> directories = new HashMap<>();

    public static HomeDirectories instance() {
        return DbxAPI.instance().getHomeDirectories();
    }

    public void put(String name, String path) {
        directories.put(name, new File(path));
    }
    public File get(String name) {
        return directories.getOrDefault(name, new File("."));
    }
    public void clear() {
        directories.clear();
    }

    public List<String> names() {
        return List.copyOf(directories.keySet());
    }
}