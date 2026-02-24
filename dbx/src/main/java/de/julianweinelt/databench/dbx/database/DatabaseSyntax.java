package de.julianweinelt.databench.dbx.database;

public abstract class DatabaseSyntax {
    public abstract String showTables();
    public abstract String showDatabases();
    public abstract String showViews();
    public abstract String showFunctions();
    public abstract String showTriggers();
    public abstract String getType();
    public abstract String getVersion();

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
    public static DatabaseSyntax MYSQL = new DatabaseSyntax() {
        @Override
        public String showTables() {
            return "SHOW FULL TABLES IN ${db} WHERE TABLE_TYPE = 'BASE TABLE';";
        }

        @Override
        public String showDatabases() {
            return "SHOW DATABASES;";
        }

        @Override
        public String showViews() {
            return "SHOW FULL TABLES IN ${db} WHERE TABLE_TYPE = 'VIEW';";
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
