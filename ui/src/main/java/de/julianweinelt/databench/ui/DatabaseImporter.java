package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.dbx.database.ADatabase;
import de.julianweinelt.databench.dbx.export.DbxArchiveReader;
import de.julianweinelt.databench.dbx.export.ImportListener;
import de.julianweinelt.databench.dbx.model.Manifest;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void importData() {
        schemas.forEach((db, tables) -> {
            listener.onLog("Importing database: " + db);

            prepareDatabase(db);

            for (ADatabase.TableInfo table : tables) {
                currentStep++;

                listener.onProgress(
                        currentStep,
                        totalSteps,
                        "Importing " + db + "." + table.name()
                );

                try {
                    listener.onLog("Importing table " + table.name());

                    archiveReader.importTableData(
                            db,
                            table.name(),
                            targetDatabase
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

        listener.onLog("Import finished successfully");
        JOptionPane.showMessageDialog(
                parent,
                "Import finished successfully",
                "Import finished",
                JOptionPane.INFORMATION_MESSAGE
        );
        parent.dispose();
    }
    private void prepareDatabase(String db) {
        listener.onLog("Preparing database '" + db + "'");
        targetDatabase.createDatabaseIfNotExists(db);
        targetDatabase.useDatabase(db);
    }
}
