package de.julianweinelt.databench.data;

import de.julianweinelt.databench.DataBench;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.util.*;

@Getter
@Setter
public class Configuration {
    private final UUID installationID = UUID.randomUUID();
    private String selectedTheme;
    private String locale;

    private String encryptionPassword;

    private Map<String, String> shortcuts = new HashMap<>();

    private boolean stoppedMaximized = false;

    private boolean sendAnonymousData = false;
    private boolean sendErrorProtocols = false;
    private boolean firstStartup = true;

    private boolean checkForUpdates = true;
    private String updateChannel = "stable";
    private final int configVersion = 2;

    public static Configuration getConfiguration() {
        return DataBench.getInstance().getConfigManager().getConfiguration();
    }

    /* --------------------------------------------------
     * Shortcut API
     * -------------------------------------------------- */

    public KeyStroke getShortcut(String action, KeyStroke defaultKey) {
        String value = shortcuts.get(action);
        if (value == null || value.isBlank()) {
            return defaultKey;
        }
        KeyStroke ks = KeyStroke.getKeyStroke(value);
        return ks != null ? ks : defaultKey;
    }

    public void setShortcut(String action, KeyStroke stroke) {
        if (stroke == null) return;
        shortcuts.put(action, toPersistedString(stroke));
    }

    public void removeShortcut(String action) {
        shortcuts.remove(action);
    }

    public boolean hasCustomShortcut(String action) {
        return shortcuts.containsKey(action);
    }

    /* --------------------------------------------------
     * Utility
     * -------------------------------------------------- */

    private String toPersistedString(KeyStroke ks) {
        StringBuilder sb = new StringBuilder();

        if ((ks.getModifiers() & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) sb.append("control ");
        if ((ks.getModifiers() & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0) sb.append("shift ");
        if ((ks.getModifiers() & java.awt.event.InputEvent.ALT_DOWN_MASK) != 0) sb.append("alt ");
        if ((ks.getModifiers() & java.awt.event.InputEvent.META_DOWN_MASK) != 0) sb.append("meta ");

        sb.append(java.awt.event.KeyEvent.getKeyText(ks.getKeyCode()));
        return sb.toString().trim();
    }
}