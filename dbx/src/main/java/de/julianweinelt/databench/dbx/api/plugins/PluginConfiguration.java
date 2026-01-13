package de.julianweinelt.databench.dbx.api.plugins;

import java.util.List;

// Wrapper class for plugin.json files
public record PluginConfiguration(
        String pluginName,
        List<String> authors,
        String version,
        String mainClass,
        String description,
        String minAPIVersion,
        boolean usesEncryption,
        boolean usesDatabase,
        boolean storesSensitiveData,
        List<String> requires
) {}