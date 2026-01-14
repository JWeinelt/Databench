package de.julianweinelt.databench.dbx.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class Manifest {
    private final String formatVersion = "1.0.0";
    private long createdAt;

    private Tool tool;
    private Source source;
    private Export export;

    public static Manifest create(String version, String dbms, String dbmsVersion, List<String> databases) {
        Manifest manifest = new Manifest();
        manifest.createdAt = System.currentTimeMillis();
        manifest.tool = Tool.create(version);
        manifest.source = new Source(dbms, dbmsVersion);
        manifest.export = new Export(databases);
        return manifest;
    }

    @Getter
    public static class Tool {
        private final String name;
        private final String version;

        protected Tool(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public static Tool create(String version) {
            return new Tool("DataBench", version);
        }
    }

    public record Source(String dbms, String dbmsVersion) {}

    @Getter
    public static class Export {
        private final String mode = "DBX";
        private final List<String> databases;

        public Export(List<String> databases) {
            this.databases = databases;
        }
        public Export(String... databases) {
            this.databases = Arrays.asList(databases);
        }
    }
}
