package de.julianweinelt.databench.dbx.api.ui.menubar;

import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.events.Event;
import de.julianweinelt.databench.dbx.api.ui.ShortcutAction;
import de.julianweinelt.databench.dbx.api.ui.ShortcutManager;
import lombok.Getter;

import javax.swing.*;

public class MenuItem extends MenuComponent<JMenuItem> {
    private final String text;
    @Getter
    private final String id;
    private final JMenuItem item;

    public MenuItem(String text, String id) {
        this.text = text;
        this.id = id;
        item = new JMenuItem(text);
        item.addActionListener(e ->
                Registry.instance().callEvent(new Event("UIMenuBarItemClickEvent").set("id", id)));
    }

    public MenuItem action(Runnable action) {
        item.addActionListener(e -> {
            action.run();
        });
        return this;
    }
    public MenuItem shortcut(String name) {
        ShortcutAction action = ShortcutManager.instance().getAction(name);
        if (action == null) throw new IllegalArgumentException("No such shortcut action: " + name);
        item.setAccelerator(action.defaultKey()); //TODO: Adapt with actual config
        return this;
    }

    @Override
    public JMenuItem create() {
        return item;
    }
}
