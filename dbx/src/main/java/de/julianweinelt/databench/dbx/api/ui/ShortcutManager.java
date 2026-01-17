package de.julianweinelt.databench.dbx.api.ui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ShortcutManager {
    private final List<ShortcutAction> actions = new ArrayList<>();

    public static ShortcutManager instance() {
        return UIService.instance().getShortcutManager();
    }

    public void register(ShortcutAction action) {
        actions.add(action);
    }

    public void register(String name, String displayName, KeyStroke stroke) {
        actions.add(new ShortcutAction() {
            @Override
            public KeyStroke defaultKey() {
                return stroke;
            }

            @Override
            public String internalName() {
                return name;
            }

            @Override
            public String displayName() {
                return displayName;
            }
        });
    }

    public ShortcutAction getAction(String name) {
        for (ShortcutAction a : actions) if (a.internalName().equals(name)) return a;
        return null;
    }
}
