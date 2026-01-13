package de.julianweinelt.databench.dbx.database;

import de.julianweinelt.databench.dbx.DbxArchiveWriter;

import java.sql.Connection;

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

    public void connect() {

    }
}