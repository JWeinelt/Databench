package de.julianweinelt.databench.dbx.backup;

import de.julianweinelt.databench.dbx.database.ADatabase;
import de.julianweinelt.databench.dbx.model.Manifest;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class DatabaseExporter {
    private final DbxArchiveWriter archiveWriter;

    private final ADatabase database;
    private final HashMap<String, List<String>> tables = new HashMap<>();

    private final ExportListener listener;

    private final JDialog parent;

    private int totalSteps;
    private int currentStep;

    public DatabaseExporter(DbxArchiveWriter archiveWriter, ADatabase database, ExportListener listener, JDialog parent) {
        this.archiveWriter = archiveWriter;
        this.database = database;
        this.listener = listener;
        this.parent = parent;
    }

    public void retrieveBasicData() {
        listener.onLog("Connecting to database...");
        if (database.connect()) listener.onLog("Connected to database...");
        else listener.onLog("Failed to connect to database...");

        for (String db : database.getDatabases()) {
            List<String> tables = database.getTables(db);
            this.tables.put(db, tables);
            listener.onLog("Found " + tables.size() + " tables in database '" + db + "'");
        }

        totalSteps = tables.values().stream()
                .mapToInt(List::size)
                .sum();

        currentStep = 0;

        listener.onLog("Total export steps: " + totalSteps);
    }


    public void createManifest() throws IOException {
        listener.onLog("Creating manifest...");

        Manifest manifest = Manifest.create(
                "1.0.0",
                database.getDatabaseProductName(),
                database.getDatabaseProductVersion(),
                tables.keySet().stream().toList()
        );

        archiveWriter.writeJson("manifest.json", manifest);

        listener.onLog("Manifest written");
    }

    public void exportData() {
        tables.forEach((db, tableList) -> {
            listener.onLog("Exporting database: " + db);
            try {
                archiveWriter.writeJson("databases/" + db + "/schema.json", database.getSchemaInfo(db));
            } catch (IOException e) {
                log.error(e.getMessage());
                listener.onError("Failed to export database details for " + db, e);
            }

            tableList.forEach(tableName -> {
                currentStep++;
                listener.onProgress(
                        currentStep,
                        totalSteps,
                        "Exporting " + db + "." + tableName
                );

                try {
                    listener.onLog("Exporting table " + tableName + "...");

                    database.setStreaming(false);
                    TableDefinition tableDef = database.extractTableMeta(db, tableName);
                    archiveWriter.writeJson("databases/" + db + "/tables/" + tableName + ".table.json", tableDef);

                    database.setStreaming(true);

                    ResultSet resultSet = database.getTableData(db, tableName);

                    archiveWriter.exportTableData(
                            db,
                            tableName,
                            resultSet
                    );


                    listener.onLog("Finished table " + tableName);
                } catch (SQLException e) {
                    listener.onError(
                            "SQL error while exporting table " + tableName,
                            e
                    );
                } catch (Exception e) {
                    listener.onError(
                            "Unexpected error while exporting table " + tableName,
                            e
                    );
                }
            });
        });

        listener.onLog("Export finished successfully");
        JOptionPane.showMessageDialog(parent, "Export finished successfully", "Export finished successfully", JOptionPane.INFORMATION_MESSAGE);
        //parent.dispose();
    }
}