package de.julianweinelt.databench.dbx.api.plugins;


import de.julianweinelt.databench.dbx.api.ui.menubar.Menu;
import de.julianweinelt.databench.dbx.api.ui.menubar.MenuItem;
import de.julianweinelt.databench.dbx.api.ui.menubar.MenuManager;
import org.jetbrains.annotations.ApiStatus;

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

        getLogger().info("Registering custom menu items.");

        MenuManager.instance().register(
                new Menu("Test", "test").child(new MenuItem("Test2").action(() ->
                        getLogger().info("Test"))),
                this);
        //TODO: Add all other built-in menus to this
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onDefineEvents() {
        getRegistry().registerEvents(this, "UIServiceEnabledEvent");
    }
}