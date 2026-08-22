package de.julianweinelt.datacat.server.model;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record AccountStatus(UUID uniqueID, String name, Color color) {
    private static final List<AccountStatus> registered = new ArrayList<>();

    public void register() {
        registered.add(this);
    }

    public static AccountStatus get(UUID uniqueID) {
        return registered.stream().filter(v -> v.uniqueID.equals(uniqueID)).findFirst().orElse(null);
    }

    public static AccountStatus get(String name) {
        return registered.stream().filter(v -> v.name.equals(name)).findFirst().orElse(null);
    }
    public static List<AccountStatus> values() {
        return registered;
    }
}