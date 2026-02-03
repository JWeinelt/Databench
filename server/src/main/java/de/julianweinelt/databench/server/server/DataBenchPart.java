package de.julianweinelt.databench.server.server;

public enum DataBenchPart {
    EDITOR("ui"),
    LAUNCHER("launcher"),
    FLOW("flow");

    public final String folder;

    DataBenchPart(String folder) {
        this.folder = folder;
    }
}
