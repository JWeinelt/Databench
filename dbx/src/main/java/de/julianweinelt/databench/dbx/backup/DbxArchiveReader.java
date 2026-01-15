package de.julianweinelt.databench.dbx.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.databench.dbx.database.ADatabase;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
public class DbxArchiveReader implements Closeable {

    private final ZipFile zipFile;
    private final Gson gson;

    public DbxArchiveReader(Path file) throws IOException {
        this.zipFile = new ZipFile(file.toFile());
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    public <T> T readJson(String path, Class<T> type) throws IOException {
        ZipEntry entry = zipFile.getEntry(path);
        if (entry == null) {
            throw new FileNotFoundException("Entry not found in DBX: " + path);
        }

        try (
                InputStream in = zipFile.getInputStream(entry);
                InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)
        ) {
            return gson.fromJson(reader, type);
        }
    }

    public boolean exists(String path) {
        return zipFile.getEntry(path) != null;
    }

    public TableDefinition getTableDefinition(String database, String table) {
        try {
            return readJson("databases/" + database + "/tables/" + table + ".table.json", TableDefinition.class);
        } catch (IOException e) {
            log.warn("Table definition not found for table {}.{}", database, table);
            return null;
        }
    }


    public void importTableData(
            String database,
            String table,
            ADatabase targetDatabase,
            ImportListener listener,
            TableDefinition def
    ) throws Exception {
        log.info("Detected table: {}.{}", database, table);
        switch (database) {
            case "information_schema", "mysql", "performance_schema", "sys" -> {
                return;
            }
        }
        String entryPath = "databases/" + database + "/data/" + table + ".data.jsonl";
        ZipEntry entry = zipFile.getEntry(entryPath);
        log.info("Starting import of table {}.{}", database, table);

        if (entry == null) {
            log.warn("No data file found for table {}.{}", database, table);
            return;
        }

        long totalLines = 0;
        try (InputStream in = zipFile.getInputStream(entry);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                totalLines++;
            }
        }

        try (
                InputStream in = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            List<Map<String, Object>> batch = new ArrayList<>();
            final int MAX_BATCH = 1000;

            long rows = 0;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                rows++;

                if (totalLines > 2000) {
                    listener.message("Importing table " + database + "/" + table + " (" + rows + "/" + totalLines + ")");
                }

                Map<String, Object> jsonMap = gson.fromJson(line, new TypeToken<Map<String, Object>>(){}.getType());
                batch.add(jsonMap);

                if (batch.size() >= MAX_BATCH) {
                    insertBatch(database, table, batch, targetDatabase, def);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                insertBatch(database, table, batch, targetDatabase, def);
            }

            log.info("Imported {} rows into table {}", rows, table);
        }
    }

    private void insertBatch(
            String db,
            String table,
            List<Map<String, Object>> batch,
            ADatabase targetDatabase,
            TableDefinition def
    ) throws SQLException {
        if (batch.isEmpty()) return;

        Map<String, Object> first = batch.get(0);
        List<String> columns = new ArrayList<>(first.keySet());

        String columnList = String.join(", ", columns);
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + db + "." + table + " (" + columnList + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = targetDatabase.prepareStatement(sql)) {
            for (Map<String, Object> row : batch) {
                for (int i = 0; i < columns.size(); i++) {
                    Object value = row.get(columns.get(i));
                    if (def.getColumn(columns.get(i)).getType().equalsIgnoreCase("datetime")) value = ((Number) value).longValue();
                    ps.setObject(i + 1, value);
                }
                ps.addBatch();
            }
            ps.executeBatch();
            log.debug("Inserted {} rows into table {}", batch.size(), table);
        }
    }

    @Override
    public void close() throws IOException {
        zipFile.close();
    }
}