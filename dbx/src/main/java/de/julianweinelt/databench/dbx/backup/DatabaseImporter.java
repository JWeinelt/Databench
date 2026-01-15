package de.julianweinelt.databench.dbx.backup;

import de.julianweinelt.databench.dbx.database.ADatabase;
import de.julianweinelt.databench.dbx.model.Manifest;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class DatabaseImporter {

    private final DbxArchiveReader archiveReader;
    private final ADatabase targetDatabase;
    private final ImportListener listener;
    private final JDialog parent;

    private Manifest manifest;

    private final Map<String, List<ADatabase.TableInfo>> schemas = new HashMap<>();

    private int totalSteps;
    private int currentStep;

    public DatabaseImporter(
            DbxArchiveReader archiveReader,
            ADatabase targetDatabase,
            ImportListener listener,
            JDialog parent
    ) {
        this.archiveReader = archiveReader;
        this.targetDatabase = targetDatabase;
        this.listener = listener;
        this.parent = parent;
    }

    public void readManifest() throws IOException {
        listener.onLog("Reading manifest...");
        manifest = archiveReader.readJson("manifest.json", Manifest.class);

        if (manifest == null) {
            throw new IOException("Manifest could not be read");
        }

        listener.onLog("Manifest loaded");
        listener.onLog("Exported with " +
                manifest.getTool().getName() + " " +
                manifest.getTool().getVersion());
    }

    public void validate() {
        listener.onLog("Validating import...");

        if (!"DBX".equalsIgnoreCase(manifest.getExport().getMode())) {
            throw new IllegalStateException(
                    "Unsupported export mode: " + manifest.getExport().getMode()
            );
        }

        if (manifest.getExport().getDatabases().isEmpty()) {
            throw new IllegalStateException("No databases found in export");
        }

        listener.onLog("Found " +
                manifest.getExport().getDatabases().size() +
                " databases to import");
    }

    public void connectTarget() {
        listener.onLog("Connecting to target database...");
        if (!targetDatabase.connect()) {
            throw new IllegalStateException("Failed to connect to target database");
        }
        listener.onLog("Connected to target database");
    }

    public void loadSchemas() throws IOException {
        listener.onLog("Loading schema information...");

        for (String db : manifest.getExport().getDatabases()) {
            listener.onLog("Loading schema for database '" + db + "'");

            ADatabase.SchemaInfo schema = archiveReader.readJson(
                    "databases/" + db + "/schema.json",
                    ADatabase.SchemaInfo.class
            );

            schemas.put(db, schema.tables());
        }

        totalSteps = schemas.values().stream()
                .mapToInt(List::size)
                .sum();

        currentStep = 0;

        listener.onLog("Total import steps: " + totalSteps);
    }

    public void ensureTableExists(
            String database,
            TableDefinition table,
            ADatabase targetDb) {

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS `")
                .append(database).append("`.`")
                .append(table.getName()).append("` (\n");

        List<String> cols = new ArrayList<>();

        for (ColumnDefinition c : table.getColumns()) {
            String col =
                    "`" + c.getName() + "` " + c.getType() +
                            (c.isNullable() ? "" : " NOT NULL") +
                            (c.isAutoIncrement() ? " AUTO_INCREMENT" : "");
            cols.add(col);
        }

        if (!table.getPrimaryKey().isEmpty()) {
            cols.add("PRIMARY KEY (" +
                    table.getPrimaryKey().stream()
                            .map(k -> "`" + k + "`")
                            .collect(Collectors.joining(", ")) +
                    ")");
        }

        sql.append(String.join(",\n", cols));
        sql.append("\n) ENGINE=").append(table.getEngine());

        try (PreparedStatement ps = targetDb.prepareStatement(sql.toString())) {
            ps.execute();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public void importData() {
        schemas.forEach((db, tables) -> {
            listener.onLog("Importing database: " + db);
            listener.message("Importing database: " + db);
            switch (db) {
                case "information_schema", "mysql", "performance_schema", "sys" -> {
                    return;
                }
            }

            prepareDatabase(db);

            for (ADatabase.TableInfo table : tables) {
                TableDefinition def = archiveReader.getTableDefinition(db, table.name());
                listener.onLog("Creating table " + db + "." + table.name());
                listener.message("Creating table " + db + "." + table.name());
                ensureTableExists(db, def, targetDatabase);
                currentStep++;

                listener.onProgress(
                        currentStep,
                        totalSteps
                );

                try {
                    listener.onLog("Importing table " + db + "." + table.name());
                    listener.message("Importing table " + db + "." + table.name());

                    archiveReader.importTableData(
                            db,
                            table.name(),
                            targetDatabase,
                            listener,
                            def
                    );

                    listener.onLog("Finished table " + table.name());
                } catch (Exception e) {
                    listener.onError(
                            "Failed to import table " + table.name(),
                            e
                    );
                }
            }
        });

        listener.message("Finished import; committing changes");
        listener.onLog("Import finished successfully");
        listener.onLog("Committing...");
        targetDatabase.commit();
        JOptionPane.showMessageDialog(
                parent,
                "Import finished successfully",
                "Import finished",
                JOptionPane.INFORMATION_MESSAGE
        );
        listener.message("Import finished successfully");
        //parent.dispose();
    }
    private void prepareDatabase(String db) {
        listener.onLog("Preparing database '" + db + "'");
        targetDatabase.createDatabaseIfNotExists(db);
        targetDatabase.useDatabase(db);
    }
}
