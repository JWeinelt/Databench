package de.julianweinelt.databench.ui;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

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
            JMenu fileMenu = new JMenu("File");
            JMenuItem openButton = new JMenuItem("Open");
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
            JMenuItem saveButton = new JMenuItem("Save");
            saveButton.setEnabled(!disable);
            JMenuItem saveAsButton = new JMenuItem("Save As");
            saveAsButton.setEnabled(!disable);
            fileMenu.add(openButton);
            fileMenu.add(saveButton);
            fileMenu.add(saveAsButton);
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
            JMenu editMenu = new JMenu("Edit");
            JMenuItem undoButton = new JMenuItem("Undo");
            undoButton.setEnabled(!disable);
            JMenuItem redoButton = new JMenuItem("Redo");
            redoButton.setEnabled(!disable);
            JMenuItem cutButton = new JMenuItem("Cut");
            cutButton.setEnabled(!disable);
            editMenu.add(undoButton);
            editMenu.add(redoButton);
            editMenu.add(cutButton);
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
            JMenuItem newQueryButton = new JMenuItem("New Query");
            newQueryButton.setEnabled(!disable);
            JMenuItem newTableButton = new JMenuItem("New Table");
            newTableButton.setEnabled(!disable);
            JMenuItem newViewButton = new JMenuItem("New View");
            newViewButton.setEnabled(!disable);
            JMenuItem newProcedureButton = new JMenuItem("New Procedure");
            newProcedureButton.setEnabled(!disable);
            JMenuItem backupButton = new JMenuItem("Backups");
            backupButton.setEnabled(!disable);
            JMenuItem adminButton = new JMenuItem("Administration");
            adminButton.setEnabled(!disable);
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
