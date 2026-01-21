package de.julianweinelt.databench.api;

public enum FileType {
    FOLDER("folder"),
    JSON("json"),
    SQL("sql"),
    TXT("txt"),
    CSV("csv"),
    XLS("xls"),
    XLSX("xlsx"),
    UNKNOWN("unknown");

    public final String displayFile;

    FileType(String displayFile) {
        this.displayFile = displayFile;
    }
}
