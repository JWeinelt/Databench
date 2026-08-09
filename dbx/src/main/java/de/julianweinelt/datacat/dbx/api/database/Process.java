package de.julianweinelt.datacat.dbx.api.database;

public record Process(String id, String user, String host, String database, String command, long time,
                      String state, String info) {}