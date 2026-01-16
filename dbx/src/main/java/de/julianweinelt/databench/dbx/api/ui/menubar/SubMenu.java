package de.julianweinelt.databench.dbx.api.ui.menubar;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SubMenu extends MenuComponent<JMenu> {
    private final String name;
    private final List<MenuComponent> children = new ArrayList<>();

    protected SubMenu(String name) {
        this.name = name;
    }
    public SubMenu add(MenuComponent child) {
        children.add(child);
        return this;
    }

    @Override
    public JMenu create() {
        JMenu menu = new JMenu(name);
        for (MenuComponent child : children) {
            menu.add(child.create());
        }
        return menu;
    }
}
