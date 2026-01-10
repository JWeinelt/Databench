package de.julianweinelt.databench.server.server;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Version {
    private final DataBenchPart part;
    private final String versionID;
    private final long creationTime;
    @Setter
    private boolean supported = true;

    public Version(DataBenchPart part, String versionID) {
        this.part = part;
        this.versionID = versionID;
        creationTime = System.currentTimeMillis();
    }
}