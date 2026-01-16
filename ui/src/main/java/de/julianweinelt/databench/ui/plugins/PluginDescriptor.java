package de.julianweinelt.databench.ui.plugins;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

public class PluginDescriptor {

    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final String version;
    @Getter
    private final String description;

    @Getter
    @Setter
    private boolean installed;
    @Getter
    @Setter
    private boolean enabled;
    @Getter
    @Setter
    private boolean updateAvailable;

    @Getter
    @Setter
    private ImageIcon icon;

    public PluginDescriptor(
            String id,
            String name,
            String version,
            String description,
            boolean installed
    ) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.installed = installed;
        this.enabled = true;
    }

}
