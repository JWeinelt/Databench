package de.julianweinelt.databench.ui;

import javax.swing.*;

public enum ShortcutAction {
    NEW_FILE("New File", KeyStroke.getKeyStroke("control N")),
    PREFERENCES("Preferences", KeyStroke.getKeyStroke("control P")),
    OPEN_FILE("Open File", KeyStroke.getKeyStroke("control O")),
    SAVE_FILE("Save File", KeyStroke.getKeyStroke("control S")),
    SAVE_FILE_AS("Save File As", KeyStroke.getKeyStroke("control shift S")),
    CLOSE_FILE("Close File", KeyStroke.getKeyStroke("control W")),
    UNDO("Undo", KeyStroke.getKeyStroke("control Z")),
    REDO("Redo", KeyStroke.getKeyStroke("control Y")),
    MANAGE_DRIVERS("Manage Drivers", KeyStroke.getKeyStroke("control shift D")),
    REFRESH_DATABASES("Refresh Databases", KeyStroke.getKeyStroke("F5")),
    EXECUTE_QUERY("Execute Query", KeyStroke.getKeyStroke("F9")),
    FORMAT_QUERY("Format Query", KeyStroke.getKeyStroke("control shift F")),
    FIND_IN_QUERY("Find in Query", KeyStroke.getKeyStroke("control F")),
    TOGGLE_BOOKMARK("Toggle Bookmark", KeyStroke.getKeyStroke("control F2")),
    NEXT_BOOKMARK("Next Bookmark", KeyStroke.getKeyStroke("F2")),
    PREVIOUS_BOOKMARK("Previous Bookmark", KeyStroke.getKeyStroke("shift F2")),
    BACKUPS_EXPORT("Export Backups", KeyStroke.getKeyStroke("control shift J")),
    BACKUPS_IMPORT("Import Backups", KeyStroke.getKeyStroke("control shift I")),
    ADMINISTRATION("Administration", KeyStroke.getKeyStroke("control shift A"));

    private final String displayName;
    private final KeyStroke defaultKey;

    ShortcutAction(String displayName, KeyStroke defaultKey) {
        this.displayName = displayName;
        this.defaultKey = defaultKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public KeyStroke getDefaultKey() {
        return defaultKey;
    }
}
