package de.julianweinelt.datacat.server.store.yarn;

import java.awt.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

public record YarnStatus(UUID id, String name, Color bg, Color fg, boolean publicVisible) {
    private static final List<YarnStatus> statuses = new ArrayList<>();

    public void register() {
        statuses.add(this);
    }

    public static List<YarnStatus> getAll() {
        return statuses;
    }
    public static YarnStatus[] values() {
        return statuses.toArray(new YarnStatus[0]);
    }
    public static YarnStatus get(UUID id) {
        return statuses.stream().filter(s -> s.id.equals(id)).findFirst().orElse(null);
    }
    public static YarnStatus get(String name) {
        return statuses.stream().filter(s -> s.name.equals(name)).findFirst().orElse(null);
    }
}