package de.julianweinelt.databench.data;


import de.julianweinelt.databench.dbx.api.plugins.DbxPlugin;
import de.julianweinelt.databench.dbx.api.ui.ShortcutManager;
import de.julianweinelt.databench.dbx.api.ui.menubar.Menu;
import de.julianweinelt.databench.dbx.api.ui.menubar.MenuItem;
import de.julianweinelt.databench.dbx.api.ui.menubar.MenuManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.ApiStatus;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import static de.julianweinelt.databench.dbx.util.LanguageManager.translate;

/**
 * The SystemPlugin is a core plugin representing the system module of the DataBench DBX plugin framework.
 * It is used to register system-level events.
 * Unloading it may lead to unexpected behavior.<br>
 * <b>DO NOT USE THIS PLUGIN INSTANCE FOR ANY REGISTRATIONS!!!</b>
 * @apiNote This class is intended for internal use only.
 * @author Julian Weinelt
 * @since 1.0.0
 */
@Slf4j
@ApiStatus.Internal
public final class SystemPlugin extends DbxPlugin {

    @Override
    public void preInit() {
    }

    @Override
    public void init() {
        getLogger().info("DBX System Module has been enabled.");
        getRegistry().registerListener(this, this);
        getRegistry().registerEvents(this, "UIMenuBarItemClickEvent");
        getRegistry().registerEvents(this, "LanguageChangeEvent");


        ShortcutManager m = ShortcutManager.instance();
        m.register("OPEN_FILE", "Open File", KeyStroke.getKeyStroke("control O"));
        m.register("SAVE_FILE", "Save File", KeyStroke.getKeyStroke("control S"));
        m.register("SAVE_FILE_AS", "Save File as", KeyStroke.getKeyStroke("control shift S"));
        m.register("PREFERENCES", "Preferences", KeyStroke.getKeyStroke("control P"));

        m.register("UNDO", "Undo", KeyStroke.getKeyStroke("control Z"));
        m.register("REDO", "Redo", KeyStroke.getKeyStroke("control Y"));

        getLogger().info("Registering custom menu items.");

        //TODO: Add all other built-in menus to this
        Menu fileMenu = new Menu(translate("menu.cat.file"), "file")
                .child(new MenuItem(translate("menu.cat.file.open"), "file_open").shortcut("OPEN_FILE").action(() -> {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Open File");
                    fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(
                            "DataBench Project Files (*.dbproj), SQL Files (*.sql)", "dbproj", "sql"));
                    fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("SQL Files (*.sql)", "sql"));
                    fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("DataBench Project Files (*.dbproj)", "dbproj"));
                    fileChooser.setAcceptAllFileFilterUsed(true);
                    fileChooser.setDialogType(JFileChooser.FILES_ONLY);
                    int returnValue = fileChooser.showOpenDialog(getMainFrame());
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        JOptionPane.showMessageDialog(getMainFrame(), "Opened: " + fileChooser.getSelectedFile().getAbsolutePath());
                        //TODO: Add logic
                    }
                }))
                .child(new MenuItem("Save", "file_save").shortcut("SAVE_FILE"))
                .child(new MenuItem("Save As", "file_save_as").shortcut("SAVE_FILE_AS"))
                .child(new MenuItem("Light Edit", "file_light_edit"))
                .separator()
                .child(new MenuItem("Preferences", "file_preferences").shortcut("PREFERENCES"))
                .child(new MenuItem("Plugins", "file_plugins"))
                .child(new MenuItem("Restart IDE", "file_restart"))
                .child(new MenuItem("Exit", "file_exit"))
                .priority(998)
                ;

        Menu editMenu = new Menu("Edit", "edit")
                .child(new MenuItem("Undo", "edit_undo").shortcut("UNDO"))
                .child(new MenuItem("Redo", "edit_redo").shortcut("REDO"))
                .child(new MenuItem("Export...", "edit_export"))
                .child(new MenuItem("Import...", "edit_import"))
                .priority(997)
                ;


        Menu helpMenu = new Menu("Help", "help")
                .child(new MenuItem("Help Index", "help_help_index").action(() -> {
                    try {
                        Desktop.getDesktop().browse(URI.create("https://dev.mysql.com/doc/refman/8.0/en/"));
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }))
                .separator()
                .child(new MenuItem("License Info", "help_license"))
                .child(new MenuItem("Report a bug", "help_bug_report").action(() -> {
                    try {
                        Desktop.getDesktop().browse(URI.create("https://github.com/JWeinelt/databench/issues/new/choose"));
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }))
                .child(new MenuItem("Locate Log Files", "help_log_files").action(() ->
                        Desktop.getDesktop().browseFileDirectory(new File("logs"))))
                .child(new MenuItem("Version Info", "help_ver_info"))
                .child(new MenuItem("Check for updates", "help_check_updates"))
                .child(new MenuItem("Show Changelog", "help_show_changelog"))
                .separator()
                .child(new MenuItem("Anonymous Data Sending...", "help_data_sending"))
                .priority(996);


        MenuManager.instance().register(this, fileMenu, editMenu, helpMenu);
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onDefineEvents() {
        getRegistry().registerEvents(this, "UIServiceEnabledEvent");
    }

    @Override
    public String getName() {
        return "System";
    }
}