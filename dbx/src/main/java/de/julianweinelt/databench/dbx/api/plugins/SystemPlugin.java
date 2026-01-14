package de.julianweinelt.databench.dbx.api.plugins;


import de.julianweinelt.databench.dbx.api.events.Event;
import de.julianweinelt.databench.dbx.api.events.Subscribe;
import de.julianweinelt.databench.dbx.api.ui.SettingsPanel;
import de.julianweinelt.databench.dbx.api.ui.UIService;
import de.julianweinelt.databench.dbx.api.ui.components.ComponentCheckbox;
import de.julianweinelt.databench.dbx.api.ui.components.ComponentComboBox;
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
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onDefineEvents() {
        getRegistry().registerEvents(this, "UIServiceEnabledEvent");
    }

    @Subscribe(value = "UIServiceEnabledEvent")
    public void onUIReady(Event event) {
        getLogger().info("Registering settings dialogs...");
        UIService service = event.get("service").asValue(UIService.class);
        SettingsPanel examplePage = new SettingsPanel("Example");
        examplePage.add(new ComponentCheckbox().label("Test"));
        examplePage.add(new ComponentComboBox().option("Test", () -> getLogger().info("Test2")).option("Test2", () -> getLogger().info("Test3")));
        service.addSettingsPanel(examplePage);
    }
}