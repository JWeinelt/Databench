package de.julianweinelt.datacat.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record LinkType(UUID linkId, String displayName, String iconName, String urlRegex) {
    public void register() {
        registered.add(this);
    }

    private static final List<LinkType> registered = new ArrayList<>();
    public static LinkType get(UUID linkId) {
        return registered.stream().filter(l -> l.linkId.equals(linkId)).findFirst().orElse(null);
    }
}
