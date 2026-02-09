package de.julianweinelt.databench.flow.util;

import de.julianweinelt.databench.dbx.api.plugins.DbxPlugin;

public class SystemPlugin extends DbxPlugin {
    @Override
    public void preInit() {

    }

    @Override
    public void init() {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onDefineEvents() {
        getRegistry().registerEvents(this,
                "FlowReadyEvent",
                "UserLoginEvent",
                "UserLoginFailedEvent",
                "UserLogoutEvent",
                "UserCreateEvent",
                "UserDisableEvent",
                "UserEnableEvent",
                "UserDeleteEvent",
                "UserRoleChangeEvent",
                "ConnectionPingEvent",
                "FlowShutdownEvent",
                "JobCreateEvent",
                "JobUpdateEvent",
                "JobDeleteEvent"
                );
    }
}
