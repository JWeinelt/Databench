package de.julianweinelt.datacat.server.store;

import de.julianweinelt.datacat.server.store.yarn.Yarn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class YarnManager {
    private static YarnManager instance;

    public static YarnManager instance() {
        return instance;
    }

    public YarnManager() {
        instance = this;
    }

    private final List<Yarn> yarns = new ArrayList<>();

    public List<Yarn> getPopularYarns() {
        return yarns;
    }

    public List<Yarn> getAllYarns() {
        return yarns;
    }

    public void addYarn(Yarn yarn) {
        yarns.add(yarn);
    }

    public static Yarn getYarn(String slug) {
        for (Yarn yarn : instance().yarns) {
            if (generateSlug(yarn.getName()).equals(slug)) {
                return yarn;
            }
        }
        return null;
    }
    public static Yarn getYarn(UUID id) {
        for (Yarn yarn : instance().yarns) {
            if (yarn.getUniqueID().equals(id)) return yarn;
        }
        return null;
    }

    public static String generateSlug(String name) {
        return name.toLowerCase().replace(" ", "-");
    }
}