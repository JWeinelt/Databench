package de.julianweinelt.databench.dbx.api.ui.menubar;

import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.events.Event;
import de.julianweinelt.databench.dbx.api.events.Subscribe;
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

    private final HashMap<String, Boolean> itemsEnabled = new HashMap<>();
    private final List<MenuActivation> activations = new ArrayList<>();

    private final HashMap<String, JMenu> menus = new HashMap<>();

    public MenuBar(JFrame frame) {
        this.frame = frame;
        bar = new JMenuBar();
        log.info("Creating menu bar");
        Registry.instance().registerListener(this, Registry.instance().getSystemPlugin());
    }

    private void resetBar() {
        bar.removeAll();
        menus.clear();
        updateMenuBar();
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
        resetBar();
        registerCustomCategories();
        return this;
        //createSQLCategory(!categoryEnabled.getOrDefault("sql", false));
        //createHelpCategory(!categoryEnabled.getOrDefault("help", false));
    }

    /*
    public void createSQLCategory(boolean disable) {
        JMenu sqlMenu;
        if (!menus.containsKey("sql")) {
            sqlMenu = new JMenu("SQL");
            JMenuItem newQueryButton = new JMenuItem(translate("menu.cat.sql.new.query"));
            newQueryButton.setEnabled(!disable);
            JMenuItem newTableButton = new JMenuItem(translate("menu.cat.sql.new.table"));
            newTableButton.setEnabled(!disable);
            JMenuItem newViewButton = new JMenuItem(translate("menu.cat.sql.new.view"));
            newViewButton.setEnabled(!disable);
            JMenuItem newProcedureButton = new JMenuItem(translate("menu.cat.sql.new.procedure"));
            newProcedureButton.setEnabled(!disable);
            JMenu backupMenu = new JMenu(translate("menu.cat.sql.backups"));
            JMenuItem export = new JMenuItem("Export");
            export.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            de.julianweinelt.databench.dbx.api.ShortcutAction.BACKUPS_EXPORT.name(),
                            de.julianweinelt.databench.dbx.api.ShortcutAction.BACKUPS_EXPORT.getDefaultKey()
                    )
            );
            export.addActionListener(e -> new ExportDialog(frame).setVisible(true)); // Temporary

            JMenuItem importItem = new JMenuItem("Import");
            importItem.addActionListener(e -> {
                new ImportDialog(frame).setVisible(true);
            });
            importItem.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            de.julianweinelt.databench.dbx.api.ShortcutAction.BACKUPS_IMPORT.name(),
                            de.julianweinelt.databench.dbx.api.ShortcutAction.BACKUPS_IMPORT.getDefaultKey()
                    )
            );

            backupMenu.add(export);
            backupMenu.add(importItem);

            JMenuItem adminButton = new JMenuItem(translate("menu.cat.sql.admin"));
            adminButton.addActionListener(e -> new AdministrationDialog(frame).setVisible(true));
            adminButton.setEnabled(!disable);
            adminButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            de.julianweinelt.databench.dbx.api.ShortcutAction.ADMINISTRATION.name(),
                            de.julianweinelt.databench.dbx.api.ShortcutAction.ADMINISTRATION.getDefaultKey()
                    )
            );

            JMenu drivers = new JMenu(translate("menu.cat.sql.drivers"));
            JMenuItem downloadDriverButton = new JMenuItem(translate("menu.cat.sql.drivers.download"));
            downloadDriverButton.addActionListener(e -> new DriverDownloadDialog(frame, false).setVisible(true));
            JMenuItem manageDriverButton = new JMenuItem(translate("menu.cat.sql.drivers.manage"));

            manageDriverButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            de.julianweinelt.databench.dbx.api.ShortcutAction.MANAGE_DRIVERS.name(),
                            de.julianweinelt.databench.dbx.api.ShortcutAction.MANAGE_DRIVERS.getDefaultKey()
                    )
            );
            manageDriverButton.addActionListener(e -> new DriverManagerDialog(frame).setVisible(true));
            drivers.add(downloadDriverButton);
            drivers.add(manageDriverButton);
            sqlMenu.add(drivers);
            sqlMenu.add(newQueryButton);
            sqlMenu.add(newTableButton);
            sqlMenu.add(newViewButton);
            sqlMenu.add(newProcedureButton);
            sqlMenu.add(backupMenu);
            sqlMenu.add(adminButton);
            bar.add(sqlMenu);
            menus.put("sql", sqlMenu);
        } else {
            sqlMenu = menus.get("sql");
            for (int i = 0; i < sqlMenu.getItemCount(); i++) {
                JMenuItem item = sqlMenu.getItem(i);
                if (item != null) {
                    item.setEnabled(!disable);
                }
            }
        }
        updateMenuBar();
    }

    public void createHelpCategory(boolean disable) {
        JMenu sqlMenu;
        if (!menus.containsKey("help")) {
            sqlMenu = new JMenu("Help");
            JMenuItem helpIndex = getHelpIndexItem("Help Index", "https://dev.mysql.com/doc/refman/8.0/en/");
            helpIndex.setAccelerator(
                    KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)
            );
            JMenuItem licenseInfo = new JMenuItem("License Info");
            licenseInfo.addActionListener(e -> ui.showLicenseInfo());
            JMenuItem reportBug = getHelpIndexItem("Report a Bug", "https://github.com/JWeinelt/databench/issues/new/choose");
            JMenuItem locateLogs = new JMenuItem("Locate Log Files");
            JMenuItem showVersionInfo = new JMenuItem("Version Info");
            JMenuItem checkUpdates = new JMenuItem("Check for Updates");
            checkUpdates.addActionListener(e -> UpdateChecker.instance().checkForUpdates(true));
            JMenuItem showChangelog = new JMenuItem("Show Changelog");
            showChangelog.addActionListener(e -> ui.showChangelog());

            locateLogs.addActionListener(e -> {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                    Desktop.getDesktop().browseFileDirectory(new File("logs"));
                }
            });
            sqlMenu.add(helpIndex);
            sqlMenu.addSeparator();
            sqlMenu.add(licenseInfo);
            sqlMenu.add(reportBug);
            sqlMenu.add(locateLogs);
            sqlMenu.add(checkUpdates);
            sqlMenu.add(showVersionInfo);
            sqlMenu.add(showChangelog);
            bar.add(sqlMenu);
            menus.put("help", sqlMenu);
        } else {
            sqlMenu = menus.get("help");
            for (int i = 0; i < sqlMenu.getItemCount(); i++) {
                JMenuItem item = sqlMenu.getItem(i);
                if (item != null) {
                    item.setEnabled(!disable);
                }
            }
        }
        updateMenuBar();
    }
*/
    public void registerCustomCategories() {
        List<Menu> men = MenuManager.instance().getAllMenus();
        men.sort(Comparator.comparingInt(Menu::getPriority));
        bar.removeAll();
        for (Menu m : men) {
            log.debug("Found menu {}", m.getCategoryName());
            menus.remove(m.getCategoryName());
            JMenu menu = m.create();
            menu.setEnabled(!getActivation(m.getCategoryName()).isDisable());
            bar.add(menu);
            menus.put(m.getCategoryName(), menu);
        }
        updateMenuBar();
    }

    private static @NotNull JMenuItem getHelpIndexItem(String name, String url) {
        JMenuItem helpIndex = new JMenuItem(name);
        helpIndex.addActionListener(e -> {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(URI.create(url));
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        });
        return helpIndex;
    }


    private void updateMenuBar() {
        frame.setJMenuBar(bar);
        frame.revalidate();
        frame.repaint();
    }

    @Subscribe(value = "UIMenuBarRevalidateEvent")
    public void revalidate(Event event) {
        log.info("Got signal to revalidate menu bar");
        updateAll();
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