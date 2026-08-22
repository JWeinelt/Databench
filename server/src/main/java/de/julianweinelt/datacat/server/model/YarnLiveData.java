package de.julianweinelt.datacat.server.model;

import java.util.UUID;

public class YarnLiveData {
    private final UUID uniqueId;

    private int downloads;

    public YarnLiveData(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }
}