package de.julianweinelt.databench.dbx.api.ui.theme;

import de.julianweinelt.databench.dbx.api.plugins.DbxPlugin;

public class Theme {
    private final DbxPlugin definingPlugin;
    private final String unlocalizedName;

    private final String themeData;

    public Theme(DbxPlugin definingPlugin, String unlocalizedName, String themeData) {
        this.definingPlugin = definingPlugin;
        this.unlocalizedName = unlocalizedName;
        this.themeData = themeData;
    }
}