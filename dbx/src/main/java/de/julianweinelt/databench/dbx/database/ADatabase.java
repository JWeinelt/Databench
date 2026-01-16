package de.julianweinelt.databench.dbx.database;

import de.julianweinelt.databench.dbx.backup.ColumnDefinition;
import de.julianweinelt.databench.dbx.backup.IndexDefinition;
import de.julianweinelt.databench.dbx.backup.TableDefinition;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Getter
public abstract class ADatabase {
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public Connection conn;


    protected ADatabase(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }
    public static ADatabase of(DatabaseType type, String host, int port, String username, String password) {
        return DatabaseRegistry.instance().instantiate(type.name(), host, port, username, password);
    }

    public abstract boolean connect();
    public void disconnect() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }
    public abstract void setStreaming(boolean streaming);

    public void useDatabase(String database) {
        try {
            if (!database.matches("[a-zA-Z0-9_]+")) {
                throw new IllegalArgumentException("Invalid database name");
            }

            conn.createStatement().execute("USE `" + database + "`");
            log.debug("Using database '{}'", database);
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    public abstract List<String> getDatabases();
    public abstract List<String> getTables(String database);
    public abstract ResultSet getTableData(String database, String table) throws SQLException;

    public abstract SchemaInfo getSchemaInfo(String database);

    public abstract String getDatabaseProductName();
    public abstract String getDatabaseProductVersion();

    public void createDatabaseIfNotExists(String db) {
        try {
            conn.createStatement().execute("CREATE SCHEMA IF NOT EXISTS `" + db + "`");
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public TableDefinition extractTableMeta(String database, String table) throws SQLException {

        List<ColumnDefinition> columns = new ArrayList<>();
        List<String> primaryKey = new ArrayList<>();
        List<IndexDefinition> indexes = new ArrayList<>();

        String engine = null;

        try (PreparedStatement ps = conn.prepareStatement("""
        SELECT ENGINE
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
        """)) {
            ps.setString(1, database);
            ps.setString(2, table);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                engine = rs.getString("ENGINE");
            }
            rs.close();
        }

        try (PreparedStatement ps = conn.prepareStatement("""
        SELECT
            COLUMN_NAME,
            COLUMN_TYPE,
            IS_NULLABLE,
            EXTRA,
            COLUMN_DEFAULT
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
        ORDER BY ORDINAL_POSITION
        """)) {
            ps.setString(1, database);
            ps.setString(2, table);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                columns.add(new ColumnDefinition(
                        rs.getString("COLUMN_NAME"),
                        rs.getString("COLUMN_TYPE"),
                        "YES".equals(rs.getString("IS_NULLABLE")),
                        rs.getString("EXTRA").contains("auto_increment"),
                        rs.getString("COLUMN_DEFAULT")
                ));
            }
            rs.close();
        }

        try (PreparedStatement ps = conn.prepareStatement("""
        SELECT COLUMN_NAME
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = ?
          AND TABLE_NAME = ?
          AND CONSTRAINT_NAME = 'PRIMARY'
        ORDER BY ORDINAL_POSITION
        """)) {
            ps.setString(1, database);
            ps.setString(2, table);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                primaryKey.add(rs.getString("COLUMN_NAME"));
            }
            rs.close();
        }

        try (PreparedStatement ps = conn.prepareStatement("""
        SELECT
            INDEX_NAME,
            NON_UNIQUE,
            COLUMN_NAME,
            SEQ_IN_INDEX
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = ?
          AND TABLE_NAME = ?
          AND INDEX_NAME <> 'PRIMARY'
        ORDER BY INDEX_NAME, SEQ_IN_INDEX
        """)) {
            ps.setString(1, database);
            ps.setString(2, table);

            ResultSet rs = ps.executeQuery();

            Map<String, List<String>> indexColumns = new LinkedHashMap<>();
            Map<String, Boolean> indexUnique = new HashMap<>();

            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                indexColumns.computeIfAbsent(name, k -> new ArrayList<>())
                        .add(rs.getString("COLUMN_NAME"));
                indexUnique.putIfAbsent(name, rs.getInt("NON_UNIQUE") == 0);
            }
            rs.close();

            for (String idx : indexColumns.keySet()) {
                indexes.add(new IndexDefinition(
                        idx,
                        indexColumns.get(idx),
                        indexUnique.get(idx)
                ));
            }
        }

        return new TableDefinition(
                table,
                engine,
                columns,
                primaryKey,
                indexes
        );
    }

    public void commit() {
        try {
            conn.commit();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }
    public void rollback() {
        try {
            conn.rollback();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }


    public record SchemaInfo(String database, String defaultCharset, String defaultCollation, List<TableInfo> tables) {}
    public record TableInfo(String name, int rowCount, String engine) {}
}