package de.julianweinelt.databench.dbx.api.ui.menubar;

import lombok.Getter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Menu extends MenuComponent<JMenu> {

    private final JMenu menu;
    private final List<MenuComponent> children = new ArrayList<>();

    @Getter
    private final String categoryName;

    public Menu(String name, String categoryName) {
        menu = new JMenu(name);
        this.categoryName = categoryName;
    }

    public Menu child(MenuComponent menuComponent) {
        children.add(menuComponent);
        return this;
    }

    @Override
    public JMenu create() {
        for (MenuComponent menuComponent : children) {
            menu.add(menuComponent.create());
        }
        return menu;
    }
}