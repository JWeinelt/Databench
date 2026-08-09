package de.julianweinelt.datacat.dbx.database;


/**
 * Defines which syntax different SQL databases use.
 * @author Julian Weinelt
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class DatabaseSyntax {
    /**
     * Defines how this database queries tables.
     * @return The SQL statement for viewing tables.
     */
    public abstract String showTables();

    /**
     * Defines how this database queries database schemas.
     * @return The SQL statement for viewing schemas.
     */
    public abstract String showDatabases();

    /**
     * Defines how this database queries views.
     * @return The SQL statement for viewing database views.
     */
    public abstract String showViews();

    /**
     * Defines how this database queries functions.
     * @return The SQL statement for viewing functions.
     */
    public abstract String showFunctions();

    /**
     * Defines how this database queries triggers.
     * @return The SQL statement for viewing triggers.
     */
    public abstract String showTriggers();

    /**
     * Defines how this database it's type.
     * @return The SQL statement for getting the database engine's name.
     */
    public abstract String getType();

    /**
     * Defines how this database it's version.
     * @return The SQL statement for getting the database engine's version.
     */
    public abstract String getVersion();

    @Deprecated
    public static DatabaseSyntax MSSQL = new DatabaseSyntax() {
        @Override
        public String showTables() {
            return """
                    SELECT TABLE_NAME
                    FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_TYPE = 'BASE TABLE';
                    """;
        }

        @Override
        public String showDatabases() {
            return "SELECT name, state_desc FROM sys.databases WHERE owner_sid <> 0x01;";
        }

        @Override
        public String showViews() {
            return """
                    SELECT TABLE_NAME
                    FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_TYPE = 'VIEW';
                    """;
        }

        @Override
        public String showFunctions() {
            return "";
        }

        @Override
        public String showTriggers() {
            return "";
        }

        @Override
        public String getType() {
            return "";
        }

        @Override
        public String getVersion() {
            return "";
        }
    };
    @Deprecated
    public static DatabaseSyntax MYSQL = new DatabaseSyntax() {
        @Override
        public String showTables() {
            return "SHOW FULL TABLES IN `${db}` WHERE TABLE_TYPE = 'BASE TABLE';";
        }

        @Override
        public String showDatabases() {
            return "SHOW DATABASES;";
        }

        @Override
        public String showViews() {
            return "SHOW FULL TABLES IN `${db}` WHERE TABLE_TYPE = 'VIEW';";
        }

        @Override
        public String showFunctions() {
            return "";
        }

        @Override
        public String showTriggers() {
            return "";
        }

        @Override
        public String getType() {
            return "SELECT @@version";
        }

        @Override
        public String getVersion() {
            return "SELECT @@version";
        }
    };
}
