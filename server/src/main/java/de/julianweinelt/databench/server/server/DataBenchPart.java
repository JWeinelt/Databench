package de.julianweinelt.databench.server.server;

public enum DataBenchPart {
    EDITOR("ui"),
    LAUNCHER("launcher"),
    WORKER("worker");

    public final String folder;

    DataBenchPart(String folder) {
        this.folder = folder;
    }
}
