package de.julianweinelt.databench.dbx.api.ui.menubar;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Menu extends MenuComponent<JMenu> {

    private final JMenu menu;
    private final List<MenuComponent> children = new ArrayList<>();
    @Getter
    private int priority = 0;

    @Getter
    private final String categoryName;

    public Menu(String name, String categoryName) {
        menu = new JMenu(name);
        this.categoryName = categoryName;
    }

    public Menu(String name, String categoryName, int priority) {
        menu = new JMenu(name);
        this.categoryName = categoryName;
        priority(priority);
    }

    public Menu priority(int priority) {
        if (priority == 999) throw new IllegalArgumentException("Priority must be lower than 999");
        this.priority = priority;
        return this;
    }

    public Menu child(MenuComponent menuComponent) {
        children.add(menuComponent);
        return this;
    }
    public Menu separator() {
        children.add(new MenuSeparator());
        return this;
    }

    @Override
    public JMenu create() {
        log.debug("Started menu creation");
        int idx = 0;
        for (MenuComponent menuComponent : children) {
            if (menuComponent instanceof MenuSeparator && idx != 0) {
                menu.addSeparator();
                log.debug("Separator");
                continue;
            }
            JMenuItem i = menuComponent.create();
            menu.add(i);

            idx++;
            log.debug("{}", idx);
        }
        log.debug("Created menu {}", getCategoryName());
        return menu;
    }
}