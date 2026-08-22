package de.julianweinelt.datacat.server.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Badge(
        UUID uniqueId,
        String name,
        String description) {

    private static final List<Badge> registered = new ArrayList<>();

    public static Optional<Badge> get(UUID id) {
        return registered.stream().filter(b -> b.uniqueId.equals(id)).findFirst();
    }
    public static Optional<Badge> get(String name) {
        return registered.stream().filter(b -> b.name.equals(name)).findFirst();
    }

    public void register() {
        registered.add(this);
    }
    public Path imagePath() {
        return Path.of(".", "sys", "badge", uniqueId.toString() + ".png");
    }
}