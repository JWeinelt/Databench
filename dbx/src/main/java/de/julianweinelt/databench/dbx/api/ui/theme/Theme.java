package de.julianweinelt.databench.dbx.api.ui.theme;

import de.julianweinelt.databench.dbx.api.plugins.DbxPlugin;
import lombok.Getter;

import javax.swing.plaf.basic.BasicLookAndFeel;

@Getter
public class Theme {
    private final DbxPlugin definingPlugin;
    private final String unlocalizedName;

    private final String themeData;
    private BasicLookAndFeel lafClass = null;

    public Theme(DbxPlugin definingPlugin, String unlocalizedName, String themeData) {
        this.definingPlugin = definingPlugin;
        this.unlocalizedName = unlocalizedName;
        this.themeData = themeData;
    }

    public Theme(DbxPlugin definingPlugin, String unlocalizedName, String themeData, BasicLookAndFeel lafClass) {
        this.definingPlugin = definingPlugin;
        this.unlocalizedName = unlocalizedName;
        this.themeData = themeData;
        this.lafClass = lafClass;
    }
}