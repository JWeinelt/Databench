package de.julianweinelt.databench.dbx.api.plugins;


import de.julianweinelt.databench.dbx.api.ui.ShortcutManager;
import de.julianweinelt.databench.dbx.api.ui.menubar.Menu;
import de.julianweinelt.databench.dbx.api.ui.menubar.MenuItem;
import de.julianweinelt.databench.dbx.api.ui.menubar.MenuManager;
import org.jetbrains.annotations.ApiStatus;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * The SystemPlugin is a core plugin representing the system module of the Caesar plugin framework.
 * It is used to register system-level events and commands.
 * Unloading it may lead to unexpected behavior.<br>
 * <b>DO NOT USE THIS PLUGIN INSTANCE FOR ANY REGISTRATIONS!!!</b>
 * @apiNote This class is intended for internal use only.
 * @author Julian Weinelt
 * @since 1.0.0
 */
@ApiStatus.Internal
public final class SystemPlugin extends DbxPlugin {

    @Override
    public void preInit() {
    }

    @Override
    public void init() {
        getLogger().info("DBX System Module has been enabled.");
        getRegistry().registerListener(this, this);
        getRegistry().registerEvents("UIMenuBarItemClickEvent");


        ShortcutManager m = ShortcutManager.instance();
        m.register("OPEN_FILE", "Open File", KeyStroke.getKeyStroke("control O"));
        m.register("SAVE_FILE", "Save File", KeyStroke.getKeyStroke("control S"));
        m.register("SAVE_FILE_AS", "Save File as", KeyStroke.getKeyStroke("control shift S"));
        m.register("PREFERENCES", "Preferences", KeyStroke.getKeyStroke("control P"));

        m.register("UNDO", "Undo", KeyStroke.getKeyStroke("control Z"));
        m.register("REDO", "Redo", KeyStroke.getKeyStroke("control Y"));

        getLogger().info("Registering custom menu items.");

        //TODO: Add all other built-in menus to this
        Menu fileMenu = new Menu("File", "file")
                .child(new MenuItem("Open", "file_open").shortcut("OPEN_FILE").action(() -> {
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
                .priority(997)
                ;

        Menu editMenu = new Menu("Edit", "edit")
                .child(new MenuItem("Undo", "edit_undo").shortcut("UNDO"))
                .child(new MenuItem("Redo", "edit_redo").shortcut("REDO"))
                .priority(998)
                ;
        MenuManager.instance().register(this, fileMenu, editMenu);
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onDefineEvents() {
        getRegistry().registerEvents(this, "UIServiceEnabledEvent");
    }
}