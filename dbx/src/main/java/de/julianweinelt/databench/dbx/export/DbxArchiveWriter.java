package de.julianweinelt.databench.dbx.export;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.julianweinelt.databench.dbx.util.GsonProvider;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DbxArchiveWriter implements Closeable {

    private final ZipOutputStream zip;
    private final Gson gson = GsonProvider.gson();

    public DbxArchiveWriter(Path target) throws IOException {
        this.zip = new ZipOutputStream(
                Files.newOutputStream(target),
                StandardCharsets.UTF_8
        );
    }

    public void writeJson(String path, Object obj) throws IOException {
        zip.putNextEntry(new ZipEntry(path));

        Writer w = new OutputStreamWriter(zip, StandardCharsets.UTF_8);

        gson.toJson(obj, w);
        w.flush();

        zip.closeEntry();
    }

    public Writer openTextEntry(String path) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        return new OutputStreamWriter(zip, StandardCharsets.UTF_8);
    }

    public void exportTableData(
            String db,
            String table,
            ResultSet rs
    ) throws Exception {

        Writer w = openTextEntry(
                "databases/" + db + "/data/" + table + ".data.jsonl"
        );

        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        while (rs.next()) {
            JsonObject row = new JsonObject();
            for (int i = 1; i <= cols; i++) {
                String name = meta.getColumnLabel(i);
                Object val = rs.getObject(i);
                row.add(name, gson.toJsonTree(val));
            }
            w.write(gson.toJson(row));
            w.write('\n');
        }

        w.flush();
        zip.closeEntry();
    }


    @Override
    public void close() throws IOException {
        zip.close();
    }
}