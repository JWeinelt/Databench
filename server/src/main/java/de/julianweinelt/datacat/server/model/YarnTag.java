package de.julianweinelt.datacat.server.model;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record YarnTag(UUID uniqueID, String name, Color color) {
    private static final List<YarnTag> registered = new ArrayList<>();

    public void register() {
        registered.add(this);
    }

    public YarnTag get(UUID uniqueID) {
        return registered.stream().filter(v -> v.uniqueID.equals(uniqueID)).findFirst().orElse(null);
    }

    public YarnTag get(String name) {
        return registered.stream().filter(v -> v.name.equals(name)).findFirst().orElse(null);
    }
    public List<YarnTag> values() {
        return registered;
    }
}