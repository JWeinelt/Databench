package de.julianweinelt.databench.dbx.api.ui.menubar;

import javax.swing.*;

public class MenuItem extends MenuComponent<JMenuItem> {
    private final String text;
    private final JMenuItem item;

    public MenuItem(String text) {
        this.text = text;
        item = new JMenuItem(text);
    }

    public MenuItem action(Runnable action) {
        item.addActionListener(e -> {
            action.run();
        });
        return this;
    }

    @Override
    public JMenuItem create() {
        return item;
    }
}
