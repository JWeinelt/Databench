package de.julianweinelt.databench.dbx.database;

public enum DatabaseType {
    MYSQL("mysql", "mysql", "jdbc:mysql://${server}/${database}" +
            "?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true", DatabaseSyntax.MYSQL),
    MARIADB("mariadb", "mariadb", "jdbc:mariadb://${server}/${database}" +
            "?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true", DatabaseSyntax.MYSQL),
    MSSQL("sqlserver", "mssql", "jdbc:sqlserver://${server};databaseName=${database};integratedSecurity=true;encrypt=false", DatabaseSyntax.MSSQL),
    POSTGRESQL("postgresql", "postgre", "jdbc:postgresql://${server}/${database}" +
            "?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true", DatabaseSyntax.MYSQL);

    public final String jdbcString;
    public final String engineName;
    public final String jdbcURL;
    public final DatabaseSyntax syntax;

    DatabaseType(String jdbcString, String engineName, String jdbcURL, DatabaseSyntax syntax) {
        this.jdbcString = jdbcString;
        this.engineName = engineName;
        this.jdbcURL = jdbcURL;
        this.syntax = syntax;
    }
}