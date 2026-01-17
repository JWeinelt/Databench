package de.julianweinelt.databench.dbx.api.ui;

import javax.swing.*;

public interface ShortcutAction {
    KeyStroke defaultKey();
    String internalName();
    String displayName();
}