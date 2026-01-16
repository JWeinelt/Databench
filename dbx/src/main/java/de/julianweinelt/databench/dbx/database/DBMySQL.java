package de.julianweinelt.databench.dbx.database;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SuppressWarnings("SqlSourceToSinkFlow")
public class DBMySQL extends ADatabase {
    public DBMySQL(String host, int port, String username, String password) {
        super(host, port, username, password);
    }

    @Override
    public boolean connect() {
        String DB_NAME = "jdbc:mysql://${server}/?useJDBCCompliantTimezoneShift=true&useLegacyDatetime" +
                "Code=false&serverTimezone=UTC&autoReconnect=true&zeroDateTimeBehavior=convertToNull";
        DB_NAME = DB_NAME.replace("${server}", getHost() + ":" + getPort());

        try {
            conn = DriverManager.getConnection(DB_NAME, getUsername(), getPassword());
            return true;
        } catch (SQLException ex) {
            // Log any exception that occurs during the connection process
            log.warn("SQL connection failed: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public void setStreaming(boolean streaming) {
        try {
            conn.setAutoCommit(!streaming);
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public List<String> getDatabases() {
        List<String> list = new ArrayList<>();
        try (PreparedStatement pS = conn.prepareStatement("SHOW DATABASES;")) {
            ResultSet s = pS.executeQuery();
            while (s.next()) list.add(s.getString(1));
        } catch (SQLException ex) {
            log.warn("SQL statement failed: {}", ex.getMessage());
        }
        return list;
    }

    @Override
    public List<String> getTables(String database) {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement pS = conn.prepareStatement("USE " + database)) {pS.execute();} catch (SQLException ignored) {}
        try (PreparedStatement pS = conn.prepareStatement(DatabaseType.MYSQL.syntax.showTables().replace("${db}", database))) {
            ResultSet rs = pS.executeQuery();
            while (rs.next()) tables.add(rs.getString(1));
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return tables;
    }

    @Override
    public ResultSet getTableData(String database, String table) throws SQLException {
        PreparedStatement pS = conn.prepareStatement(
                "SELECT * FROM " + database + "." + table,
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY
        );
        pS.setFetchSize(Integer.MIN_VALUE);

        return pS.executeQuery();
    }

    @Override
    public SchemaInfo getSchemaInfo(String database) {
        String charset = "";
        String collation = "";
        try (PreparedStatement pS = conn.prepareStatement("""
            SELECT
                DEFAULT_CHARACTER_SET_NAME AS defaultCharset,
                DEFAULT_COLLATION_NAME     AS defaultCollation
            FROM information_schema.SCHEMATA
            WHERE SCHEMA_NAME = ?;
            """)) {
            pS.setString(1, database);
            ResultSet set = pS.executeQuery();
            if (set.next()) {
                charset = set.getString(1);
                collation = set.getString(2);
            } else {
                return null;
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        try (PreparedStatement pS = conn.prepareStatement("""
        SELECT
            TABLE_NAME   AS name,
            TABLE_ROWS   AS rowCount,
            ENGINE       AS engine
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE';
        
        """)) {
           pS.setString(1, database);
           ResultSet set = pS.executeQuery();
           List<TableInfo> info = new ArrayList<>();
           while (set.next()) {
               info.add(new TableInfo(set.getString(1), set.getInt(2), set.getString(3)));
           }
           return new SchemaInfo(database, charset, collation, info);
        } catch (SQLException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public String getDatabaseProductName() {
        try (PreparedStatement pS = conn.prepareStatement("SELECT @@version_comment;")) {
            ResultSet set = pS.executeQuery();
            if (set.next()) {
                return set.getString(1);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return "MySQL - Unknown Edition";
    }

    @Override
    public String getDatabaseProductVersion() {
        try (PreparedStatement pS = conn.prepareStatement("SELECT VERSION();")) {
            ResultSet set = pS.executeQuery();
            if (set.next()) {
                return set.getString(1);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return "unknown";
    }
}
