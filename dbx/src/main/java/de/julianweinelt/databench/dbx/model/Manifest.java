package de.julianweinelt.databench.dbx.model;

import java.util.List;

public class Manifest {

    public String formatVersion = "1.0.0";
    public String createdAt;

    public Tool tool;
    public Source source;
    public Export export;

    public static class Tool {
        public String name;
        public String version;
    }

    public static class Source {
        public String dbms;
        public String dbmsVersion;
    }

    public static class Export {
        public String mode = "DATABENCH";
        public List<String> databases;
    }
}
