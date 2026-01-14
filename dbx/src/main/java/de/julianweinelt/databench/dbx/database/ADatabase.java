package de.julianweinelt.databench.dbx.database;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

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
    public static ADatabase of(String host, int port, String username, String password, String db) {
        return new DBMySQL(host, port, username, password, db);
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

    public abstract List<String> getDatabases();
    public abstract List<String> getTables(String database);
    public abstract ResultSet getTableData(String database, String table) throws SQLException;

    public abstract SchemaInfo getSchemaInfo(String database);

    public abstract String getDatabaseProductName();
    public abstract String getDatabaseProductVersion();


    public record SchemaInfo(String database, String defaultCharset, String defaultCollation, List<TableInfo> tables) {}
    public record TableInfo(String name, int rowCount, String engine) {}
}