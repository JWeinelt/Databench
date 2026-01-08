package de.julianweinelt.databench.api;

public enum DatabaseType {
    MYSQL("mysql", "mysql"),
    MARIADB("mariadb", "mariadb"),
    MSSQL("sqlserver", "mssql"),
    POSTGRESQL("postgresql", "postgre");

    public final String jdbcString;
    public final String engineName;

    DatabaseType(String jdbcString, String engineName) {
        this.jdbcString = jdbcString;
        this.engineName = engineName;
    }
}