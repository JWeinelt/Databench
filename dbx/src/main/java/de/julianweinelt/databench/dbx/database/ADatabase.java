package de.julianweinelt.databench.dbx.database;

import de.julianweinelt.databench.dbx.DbxArchiveWriter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public abstract class ADatabase {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final DbxArchiveWriter archiveWriter;

    public Connection conn;


    protected ADatabase(String host, int port, String username, String password, DbxArchiveWriter archiveWriter) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.archiveWriter = archiveWriter;
    }

    public abstract void connect();
    public void disconnect() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }
}