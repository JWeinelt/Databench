package de.julianweinelt.databench.dbx.export;

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


    public void importTableData(
            String database,
            String table,
            ADatabase targetDatabase
    ) throws Exception {
        String entryPath = "databases/" + database + "/data/" + table + ".data.jsonl";
        ZipEntry entry = zipFile.getEntry(entryPath);

        if (entry == null) {
            log.warn("No data file found for table {}.{}", database, table);
            return;
        }

        targetDatabase.useDatabase(database);

        try (
                InputStream in = zipFile.getInputStream(entry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            List<Map<String, Object>> batch = new ArrayList<>();
            final int MAX_BATCH = 1000;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                Map<String, Object> jsonMap = gson.fromJson(line, new TypeToken<Map<String, Object>>(){}.getType());
                batch.add(jsonMap);

                if (batch.size() >= MAX_BATCH) {
                    insertBatch(table, batch, targetDatabase);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                insertBatch(table, batch, targetDatabase);
            }
        }
    }

    private void insertBatch(String table, List<Map<String, Object>> batch, ADatabase targetDatabase) throws SQLException {
        if (batch.isEmpty()) return;

        Map<String, Object> first = batch.get(0);
        List<String> columns = new ArrayList<>(first.keySet());

        String columnList = String.join(", ", columns);
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + table + " (" + columnList + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = targetDatabase.prepareStatement(sql)) {
            for (Map<String, Object> row : batch) {
                for (int i = 0; i < columns.size(); i++) {
                    Object value = row.get(columns.get(i));
                    ps.setObject(i + 1, value);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public void close() throws IOException {
        zipFile.close();
    }
}