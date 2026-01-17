package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.events.Event;
import de.julianweinelt.databench.dbx.api.events.Subscribe;
import de.julianweinelt.databench.dbx.api.ui.menubar.Menu;
import de.julianweinelt.databench.dbx.api.ui.menubar.MenuManager;
import de.julianweinelt.databench.service.UpdateChecker;
import de.julianweinelt.databench.ui.admin.AdministrationDialog;
import de.julianweinelt.databench.ui.driver.DriverDownloadDialog;
import de.julianweinelt.databench.ui.driver.DriverManagerDialog;
import de.julianweinelt.databench.ui.plugins.PluginDialog;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static de.julianweinelt.databench.ui.LanguageManager.translate;

@Slf4j
public class MenuBar {
    private final JFrame frame;
    private final BenchUI ui;

    private final JMenuBar bar;

    private final HashMap<String, Boolean> categoryEnabled = new HashMap<>();

    private final HashMap<String, JMenu> menus = new HashMap<>();

    public MenuBar(JFrame frame, BenchUI ui) {
        this.frame = frame;
        this.ui = ui;
        bar = new JMenuBar();
        log.info("Creating menu bar");
        Registry.instance().registerListener(this, Registry.instance().getSystemPlugin());
    }

    private void resetBar() {
        bar.removeAll();
        menus.clear();
        updateMenuBar();
    }

    public MenuBar enable(String category) {
        categoryEnabled.put(category, true);
        updateAll();
        return this;
    }

    public MenuBar disable(String category) {
        categoryEnabled.put(category, false);
        updateAll();
        return this;
    }

    public void updateAll() {
        resetBar();
        createSQLCategory(!categoryEnabled.getOrDefault("sql", false));
        createHelpCategory(!categoryEnabled.getOrDefault("help", false));
        registerCustomCategories();
    }

    public void createEditCategory(boolean disable) {
        JMenu editMenu;
        if (!menus.containsKey("edit")) {
            editMenu = new JMenu(translate("menu.cat.edit"));
            JMenuItem undoButton = new JMenuItem(translate("menu.cat.edit.undo"));
            undoButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            de.julianweinelt.databench.dbx.api.ShortcutAction.UNDO.name(),
                            de.julianweinelt.databench.dbx.api.ShortcutAction.UNDO.getDefaultKey()
                    )
            );
            undoButton.setEnabled(!disable);
            JMenuItem redoButton = new JMenuItem(translate("menu.cat.edit.redo"));
            redoButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            de.julianweinelt.databench.dbx.api.ShortcutAction.REDO.name(),
                            de.julianweinelt.databench.dbx.api.ShortcutAction.REDO.getDefaultKey()
                    )
            );
            redoButton.setEnabled(!disable);
            JMenuItem cutButton = new JMenuItem(translate("menu.cat.edit.cut"));
            cutButton.setAccelerator(
                    KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK)
            );
            cutButton.setEnabled(!disable);
            editMenu.add(undoButton);
            editMenu.add(redoButton);
            editMenu.add(cutButton);
            bar.add(editMenu);
            menus.put("edit", editMenu);
        } else {
            editMenu = menus.get("edit");
            for (int i = 0; i < editMenu.getItemCount(); i++) {
                JMenuItem item = editMenu.getItem(i);
                if (item != null) {
                    item.setEnabled(!disable);
                }
            }
        }
        updateMenuBar();
    }

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

    public void registerCustomCategories() {
        List<Menu> men = MenuManager.instance().getAllMenus();
        men.sort(Comparator.comparingInt(Menu::getPriority));
        for (Menu m : men) {
            log.debug("Found menu {}", m.getCategoryName());
            JMenu menu = m.create();
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

    @Subscribe(value = "UIMenuBarItemClickEvent")
    public void onMenuItemClick(Event event) {
        String menuID = event.get("id").asString();
        switch (menuID) {
            case "file_preferences" -> new SettingsDialog(frame).setVisible(true);
            case "file_plugins" -> new PluginDialog(frame).setVisible(true);
            case "file_light_edit" -> {
                ui.createLightEdit();
                //TODO: Disable button
            }
        }
    }
}