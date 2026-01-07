package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.ui.admin.AdministrationDialog;
import de.julianweinelt.databench.ui.driver.DriverDownloadDialog;
import de.julianweinelt.databench.ui.driver.DriverManagerDialog;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static de.julianweinelt.databench.ui.LanguageManager.translate;

@Slf4j
public class MenuBar {
    private final JFrame frame;
    private final BenchUI ui;

    private JMenuBar bar;

    private final HashMap<String, Boolean> categoryEnabled = new HashMap<>();

    private final HashMap<String, JMenu> menus = new HashMap<>();

    public MenuBar(JFrame frame, BenchUI ui) {
        this.frame = frame;
        this.ui = ui;
        bar = new JMenuBar();
        createFileCategory(false);
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
        createFileCategory(!categoryEnabled.getOrDefault("file", false));
        createEditCategory(!categoryEnabled.getOrDefault("edit", false));
        createSQLCategory(!categoryEnabled.getOrDefault("sql", false));
        createHelpCategory(!categoryEnabled.getOrDefault("help", false));
    }

    public void createFileCategory(boolean disable) {
        if (!menus.containsKey("file")) {
            JMenu fileMenu = new JMenu(translate("menu.cat.file"));
            JMenuItem openButton = new JMenuItem(translate("menu.cat.file.open"));
            openButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            ShortcutAction.OPEN_FILE.name(),
                            ShortcutAction.OPEN_FILE.getDefaultKey()
                    )
            );
            openButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Open File");
                fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(
                        "DataBench Project Files (*.dbproj), SQL Files (*.sql)", "dbproj", "sql"));
                fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("SQL Files (*.sql)", "sql"));
                fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("DataBench Project Files (*.dbproj)", "dbproj"));
                fileChooser.setAcceptAllFileFilterUsed(true);
                fileChooser.setDialogType(JFileChooser.FILES_ONLY);
                int returnValue = fileChooser.showOpenDialog(frame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(frame, "Opened: " + fileChooser.getSelectedFile().getAbsolutePath());
                }
            });
            JMenuItem saveButton = new JMenuItem(translate("menu.cat.file.save"));
            saveButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            ShortcutAction.SAVE_FILE.name(),
                            ShortcutAction.SAVE_FILE.getDefaultKey()
                    )
            );
            saveButton.setEnabled(!disable);
            JMenuItem saveAsButton = new JMenuItem(translate("menu.cat.file.saveAs"));
            saveAsButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            ShortcutAction.SAVE_FILE_AS.name(),
                            ShortcutAction.SAVE_FILE_AS.getDefaultKey()
                    )
            );
            saveAsButton.setEnabled(!disable);

            JMenuItem lightEditButton = new JMenuItem(translate("menu.cat.file.lightEdit"));
            lightEditButton.addActionListener(e -> {
                ui.createLightEdit();
                lightEditButton.setEnabled(!ui.hasLightEdit());
            });
            fileMenu.add(openButton);
            fileMenu.add(saveButton);
            fileMenu.add(saveAsButton);
            fileMenu.add(lightEditButton);
            bar.add(fileMenu);
            menus.put("file", fileMenu);
            updateMenuBar();
        } else {
            JMenu fileMenu = menus.get("file");
            for (int i = 0; i < fileMenu.getItemCount(); i++) {
                JMenuItem item = fileMenu.getItem(i);
                if (item != null) {
                    item.setEnabled(!disable);
                }
            }
            updateMenuBar();
        }
    }

    public void createEditCategory(boolean disable) {
        if (!menus.containsKey("edit")) {
            JMenu editMenu = new JMenu(translate("menu.cat.edit"));
            JMenuItem undoButton = new JMenuItem(translate("menu.cat.edit.undo"));
            undoButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            ShortcutAction.UNDO.name(),
                            ShortcutAction.UNDO.getDefaultKey()
                    )
            );
            undoButton.setEnabled(!disable);
            JMenuItem redoButton = new JMenuItem(translate("menu.cat.edit.redo"));
            redoButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            ShortcutAction.REDO.name(),
                            ShortcutAction.REDO.getDefaultKey()
                    )
            );
            redoButton.setEnabled(!disable);
            JMenuItem cutButton = new JMenuItem(translate("menu.cat.edit.cut"));
            cutButton.setAccelerator(
                    KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK)
            );
            cutButton.setEnabled(!disable);
            JMenuItem preferencesButton = new JMenuItem(translate("menu.cat.edit.preferences"));
            preferencesButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            ShortcutAction.PREFERENCES.name(),
                            ShortcutAction.PREFERENCES.getDefaultKey()
                    )
            );
            preferencesButton.addActionListener(e -> new SettingsDialog(frame).setVisible(true));
            editMenu.add(undoButton);
            editMenu.add(redoButton);
            editMenu.add(cutButton);
            editMenu.add(preferencesButton);
            bar.add(editMenu);
            menus.put("edit", editMenu);
            updateMenuBar();
        } else {
            JMenu editMenu = menus.get("edit");
            for (int i = 0; i < editMenu.getItemCount(); i++) {
                JMenuItem item = editMenu.getItem(i);
                if (item != null) {
                    item.setEnabled(!disable);
                }
            }
            updateMenuBar();
        }
    }

    public void createSQLCategory(boolean disable) {
        if (!menus.containsKey("sql")) {
            JMenu sqlMenu = new JMenu("SQL");
            JMenuItem newQueryButton = new JMenuItem(translate("menu.cat.sql.new.query"));
            newQueryButton.setEnabled(!disable);
            JMenuItem newTableButton = new JMenuItem(translate("menu.cat.sql.new.table"));
            newTableButton.setEnabled(!disable);
            JMenuItem newViewButton = new JMenuItem(translate("menu.cat.sql.new.view"));
            newViewButton.setEnabled(!disable);
            JMenuItem newProcedureButton = new JMenuItem(translate("menu.cat.sql.new.procedure"));
            newProcedureButton.setEnabled(!disable);
            JMenuItem backupButton = new JMenuItem(translate("menu.cat.sql.backups"));
            backupButton.setEnabled(!disable);
            backupButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            ShortcutAction.BACKUPS.name(),
                            ShortcutAction.BACKUPS.getDefaultKey()
                    )
            );
            JMenuItem adminButton = new JMenuItem(translate("menu.cat.sql.administration"));
            adminButton.addActionListener(e -> new AdministrationDialog(frame).setVisible(true));
            adminButton.setEnabled(!disable);
            adminButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            ShortcutAction.ADMINISTRATION.name(),
                            ShortcutAction.ADMINISTRATION.getDefaultKey()
                    )
            );

            JMenu drivers = new JMenu(translate("menu.cat.sql.drivers"));
            JMenuItem downloadDriverButton = new JMenuItem(translate("menu.cat.sql.drivers.download"));
            downloadDriverButton.addActionListener(e -> new DriverDownloadDialog(frame, false).setVisible(true));
            JMenuItem manageDriverButton = new JMenuItem(translate("menu.cat.sql.drivers.manage"));

            manageDriverButton.setAccelerator(
                    Configuration.getConfiguration().getShortcut(
                            ShortcutAction.MANAGE_DRIVERS.name(),
                            ShortcutAction.MANAGE_DRIVERS.getDefaultKey()
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
            sqlMenu.add(backupButton);
            sqlMenu.add(adminButton);
            bar.add(sqlMenu);
            menus.put("sql", sqlMenu);
            updateMenuBar();
        } else {
            JMenu sqlMenu = menus.get("sql");
            for (int i = 0; i < sqlMenu.getItemCount(); i++) {
                JMenuItem item = sqlMenu.getItem(i);
                if (item != null) {
                    item.setEnabled(!disable);
                }
            }
            updateMenuBar();
        }
    }

    public void createHelpCategory(boolean disable) {
        if (!menus.containsKey("help")) {
            JMenu sqlMenu = new JMenu("Help");
            JMenuItem helpIndex = new JMenuItem("Help Index");
            helpIndex.addActionListener(e -> {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(URI.create("https://dev.mysql.com/doc/refman/8.0/en/"));
                    } catch (IOException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
            });
            helpIndex.setAccelerator(
                    KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)
            );
            JMenuItem licenseInfo = new JMenuItem("License Info");
            licenseInfo.addActionListener(e -> ui.showLicenseInfo());
            JMenuItem reportBug = new JMenuItem("Report a Bug");
            reportBug.addActionListener(e -> {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(URI.create("https://github.com/JWeinelt/databench/issues/new/choose"));
                    } catch (IOException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
            });
            JMenuItem locateLogs = new JMenuItem("Locate Log Files");

            locateLogs.addActionListener(e -> {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                    Desktop.getDesktop().browseFileDirectory(new File("."));
                }
            });
            sqlMenu.add(helpIndex);
            sqlMenu.addSeparator();
            sqlMenu.add(licenseInfo);
            sqlMenu.add(reportBug);
            sqlMenu.add(locateLogs);
            bar.add(sqlMenu);
            menus.put("help", sqlMenu);
            updateMenuBar();
        } else {
            JMenu sqlMenu = menus.get("help");
            for (int i = 0; i < sqlMenu.getItemCount(); i++) {
                JMenuItem item = sqlMenu.getItem(i);
                if (item != null) {
                    item.setEnabled(!disable);
                }
            }
            updateMenuBar();
        }
    }


    private void updateMenuBar() {
        frame.setJMenuBar(bar);
        frame.revalidate();
        frame.repaint();
    }
}
