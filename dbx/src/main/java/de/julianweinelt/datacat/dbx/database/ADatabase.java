package de.julianweinelt.datacat.dbx.database;

import de.julianweinelt.datacat.dbx.api.database.Process;
import de.julianweinelt.datacat.dbx.api.database.User;
import de.julianweinelt.datacat.dbx.api.database.UserPrivilege;
import de.julianweinelt.datacat.dbx.api.exceptions.DatabaseSchemaNotFoundException;
import de.julianweinelt.datacat.dbx.backup.ColumnDefinition;
import de.julianweinelt.datacat.dbx.backup.IndexDefinition;
import de.julianweinelt.datacat.dbx.backup.TableDefinition;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

@SuppressWarnings("SqlDialectInspection")
@Slf4j
@Getter
public abstract class ADatabase {
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public Connection conn;
    private final DatabaseMetaData metaData;


    protected ADatabase(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        metaData = DatabaseRegistry.instance().getMeta(internalName());
    }
    public static ADatabase of(String type, String host, int port, String username, String password) {
        return DatabaseRegistry.instance().instantiate(type, host, port, username, password);
    }

    public abstract String internalName();

    // CONNECTION

    /**
     * Connect to the database using the default parameters of this database type
     * @return <code>true</code> if connection was successful, otherwise <code>false</code>
     */
    public boolean connect() {
        try {
            if (conn == null || conn.isClosed()) return connect(metaData.defaultParameters());
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            log.error("SQL connection failed: {}", e.getMessage());
            return connect(metaData.defaultParameters());
        }
    }

    public Connection connection() {
        return conn;
    }

    /**
     * Connect to the database using custom parameters
     * @param builder A {@link ParameterBuilder} object containing all parameters
     * @return <code>true</code> if connection was successful, otherwise <code>false</code>
     */
    public boolean connect(ParameterBuilder builder) {
        String DB_NAME = metaData.jdbcURL().replace("${server}", host + ":" + port);
        DB_NAME = DB_NAME.replace("${database}", "")
                .replace("${parameters}", metaData.parameters(builder.build()));

        try {
            conn = DriverManager.getConnection(DB_NAME, getUsername(), getPassword());
            return true;
        } catch (SQLException ex) {
            // Log any exception that occurs during the connection process
            log.warn("SQL connection failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Disconnect from database
     */
    public void disconnect() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Set the database connecting to streaming mode.
     * @param streaming streaming mode
     * @implNote Some databases don't support streaming.
     */
    public abstract void setStreaming(boolean streaming);

    /**
     * Execute the <code>USE [dbname];</code> statement on the database connection.
     * @param database The name of the database to use
     * @throws IllegalArgumentException if the database name is invalid
     * @throws DatabaseSchemaNotFoundException if the database doesn't exist
     */
    public void useDatabase(String database) {
        try {
            if (!database.matches("[a-zA-Z0-9_]+")) {
                throw new IllegalArgumentException("Invalid database name");
            }

            conn.createStatement().execute("USE `" + database + "`");
            log.debug("Using database '{}'", database);
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new DatabaseSchemaNotFoundException(database);
        }
    }

    /**
     * Prepare a statement for execution.
     * @param sql The SQL statement(s).
     * @return A {@link PreparedStatement} for use with escaping parameters
     * @throws SQLException When something went wrong
     * @apiNote You may not mix Updating/Inserting statements with queries.
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    /**
     * Get all database schemas on this database server.
     * @return A {@link List} of {@link String} objects with the names of the database schemas
     */
    public abstract List<String> getDatabases();

    /**
     * Get all table names of a specific schema.
     * @param database The schema name as a {@link String}
     * @return A {@link List} of {@link String} objects with the names of the tables
     * @apiNote This will only return <b>tables</b>, no <b>views.</b>
     */
    public abstract List<String> getTables(String database);

    /**
     * Get all view names of a specific schema.
     * @param database The schema name as a {@link String}
     * @return A {@link List} of {@link String} objects with the names of the views
     * @apiNote This will only return <b>views</b>, no <b>tables.</b>
     */
    public abstract List<String> getViews(String database);
    public abstract List<String> getStoredProcedures(String database);
    public abstract List<String> getTriggers(String database);
    public abstract ResultSet getTableData(String database, String table) throws SQLException;

    public abstract SchemaInfo getSchemaInfo(String database);

    /**
     * Loads all processes currently executed by the database server.
     * @return A list of {@link Process} objects representing all running processes.<br>Returns <code>null</code>
     * if the database does not support such queries.
     * @implNote This should return the result of e.g., <code>show processlist;</code> in MariaDB,
     * just return <code>if it is not supported by the database</code>
     */
    public abstract List<Process> getProcessList();

    /**
     * Loads all users that are registered in the database
     * @return A List of {@link User} objects
     */
    public abstract List<User> getUsers();
    public abstract UserPrivilege getUserPrivilege(String username, String host);

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

    public static class ParameterBuilder {
        private final Map<String, String> parameters = new LinkedHashMap<>();

        public ParameterBuilder parameter(String name, String value) {
            parameters.put(name, value);
            return this;
        }
        public Map<String, String> build() {
            return parameters;
        }
    }
}