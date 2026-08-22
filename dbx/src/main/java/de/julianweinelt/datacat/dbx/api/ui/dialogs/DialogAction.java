package de.julianweinelt.datacat.dbx.api.ui.dialogs;

import de.julianweinelt.datacat.dbx.api.plugins.DbxPlugin;
import lombok.Getter;

@Getter
public abstract class DialogAction {
    private final String name;
    private final DbxPlugin plugin;

    public DialogAction(String name, DbxPlugin plugin) {
        this.name = name;
        this.plugin = plugin;
    }

    public abstract void call();

    public abstract String buttonName();
    public abstract boolean translateName();
}
