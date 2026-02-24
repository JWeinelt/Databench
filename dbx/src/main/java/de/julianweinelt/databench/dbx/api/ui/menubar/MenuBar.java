package de.julianweinelt.databench.dbx.api.ui.menubar;

import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.events.Event;
import de.julianweinelt.databench.dbx.api.events.Subscribe;
import de.julianweinelt.databench.dbx.api.plugins.DbxPlugin;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class MenuBar {
    private final JFrame frame;

    private final JMenuBar bar;

    private final List<MenuActivation> activations = new ArrayList<>();

    public MenuBar(JFrame frame, DbxPlugin plugin) {
        this.frame = frame;
        bar = new JMenuBar();
        log.info("Creating menu bar");
        Registry.instance().registerListener(this, plugin);
    }

    private void resetBar() {
        log.info("Resetting menu bar");
        bar.removeAll();
        updateMenuBar();
        log.info("Done resetting menu bar");
    }

    public MenuActivation getActivation(String category) {
        for (MenuActivation a : activations) if (a.getCategory().equals(category)) return a;
        MenuActivation a = new MenuActivation(category);
        activations.add(a);
        return a;
    }

    public MenuBar enable(String category) {
        getActivation(category).setDisable(false);
        return this;
    }
    public MenuBar enable(String... categories) {
        for (String s : categories) getActivation(s).setDisable(false);
        return this;
    }

    public MenuBar enable(String category, int idx) {
        getActivation(category).getDisabledItems().remove(idx);
        return this;
    }

    public MenuBar disable(String category) {
        getActivation(category).setDisable(true);
        return this;
    }
    public MenuBar disable(String... categories) {
        for (String s : categories) getActivation(s).setDisable(true);
        return this;
    }

    public MenuBar disable(String category, int idx) {
        getActivation(category).getDisabledItems().add(idx);
        return this;
    }

    public MenuBar updateAll() {
        return updateAll(true);
    }

    public MenuBar updateAll(boolean recreate) {
        resetBar();
        if (recreate) registerCustomCategories();
        return this;
    }

    public void registerCustomCategories() {
        List<Menu> men = MenuManager.instance().getAllMenus();
        men.sort(Comparator.comparingInt(Menu::getPriority).reversed());
        bar.removeAll();
        log.info("Cleared menu bar, adding custom menus:");
        for (Menu m : men) {
            log.debug("Found menu {}", m.getCategoryName());
            JMenu menu = m.create();
            menu.setEnabled(!getActivation(m.getCategoryName()).isDisable());
            bar.add(menu);
        }
        updateMenuBar();
    }


    private void updateMenuBar() {
        log.info("Updating menu bar");
        if (!frame.isVisible()) {
            log.info("Frame is not visible, skipping update");
            return;
        }
        frame.setJMenuBar(bar);
        log.info("Revalidating frame");
        frame.revalidate();
        log.info("Repainting frame");
        frame.repaint();
        log.info("Done updating menu bar");
    }

    @Subscribe(value = "UIMenuBarRevalidateEvent")
    public void revalidate(Event event) {
        log.info("Got signal to revalidate menu bar");
        updateAll(event.get("shutdown").asBoolean());
    }

    @Getter
    @Setter
    public static class MenuActivation {
        private final String category;
        private boolean disable;
        private final List<Integer> disabledItems = new ArrayList<>();

        public MenuActivation(String category) {
            this.category = category;
        }
    }
}