package de.julianweinelt.datacat.flow.job;

import java.util.UUID;

public class Job {
    private final UUID uniqueID;

    public Job(UUID uniqueID) {
        this.uniqueID = uniqueID;
    }
}
