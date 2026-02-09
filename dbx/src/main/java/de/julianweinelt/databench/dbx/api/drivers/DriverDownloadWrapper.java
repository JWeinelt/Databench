package de.julianweinelt.databench.dbx.api.drivers;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
public class DriverDownloadWrapper {
    public static String latestVersion(String db) {
        return switch (db) {
            case "mysql" -> "9.5.0";
            case "mariadb" -> "3.4.2";
            case "mssql" -> "13.2.1";
            case "postgresql" -> "42.7.8";
            default -> "unknown";
        };
    }
    public static DriverDownload getForDB(String db, String version) {
        switch (db) {
            case "mysql" -> {
                if (version.equals("9.5.0"))
                    return new DriverDownload("https://cdn.mysql.com/Downloads/Connector-J/mysql-connector-j-9.5.0.tar.gz", true, "mysql.tar.gz");
                return new DriverDownload("https://cdn.mysql.com/Downloads/Connector-J/mysql-connector-j-" + version + ".tar.gz", true, "mysql.tar.gz");
            }
            case "mariadb" -> {
                String id = switch (version) {
                    case "3.4.2" -> "4234097";
                    case "2.7.13" -> "4550298";
                    default -> "4550269";
                };
                return new DriverDownload("https://dlm.mariadb.com/"  + id + "/Connectors/java/connector-java-"
                        + version + "/mariadb-java-client-" + version + ".jar", false, "mariadb.jar");
            }
            case "mssql" -> {
                return new DriverDownload(
                        "https://github.com/microsoft/mssql-jdbc/releases/download/v" + version + "/mssql-jdbc-" + version + ".jre11.jar",
                        false, "mssql.jar"
                );
            }
            case "postgresql" -> {
                return new DriverDownload(
                        "https://github.com/pgjdbc/pgjdbc/releases/download/REL%s/postgresql-%s.jar".formatted(version, version),
                        false, "postgresql.jar"
                );
            }
        }
        return null;
    }

    public static void postProcess(File file) {
        File temp = new File("drivers/tmp");
        if (file.getName().equals("mysql.tar.gz")) {
            log.info("Extracting driver {}", file.getName());
            File[] subFiles = temp.listFiles();
            if (subFiles == null) return;
            for (File f : subFiles) {
                if (f.getName().contains("mysql-connector")) {
                    File[] sub1 = f.listFiles();
                    if ( sub1 == null) return;
                    for (File f1 : sub1) {
                        if (f1.getName().endsWith(".jar") && f1.getName().startsWith("mysql-connector")) {
                            try {
                                Files.copy(f1.toPath(), new File("drivers", "mysql.jar").toPath());
                            } catch (IOException e) {
                                log.error(e.getMessage(), e);
                            }
                            temp.delete();
                            file.delete();
                            log.info("Driver {} extracted successfully", file.getName());
                            return;
                        }
                    }
                }
            }
        }
    }

    public record DriverDownload(String url, boolean zipped, String fileName) {}
}