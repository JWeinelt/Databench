package de.julianweinelt.databench.dbx.database.providers;

import de.julianweinelt.databench.dbx.database.ADatabase;
import de.julianweinelt.databench.dbx.database.DatabaseMetaData;
import de.julianweinelt.databench.dbx.database.DatabaseSyntax;

import java.util.Map;

public class DBMetaMySQL implements DatabaseMetaData {
    @Override
    public String jdbcString() {
        return "mysql";
    }

    @Override
    public String jdbcURL() {
        return "jdbc:mysql://${server}/${database}${parameters}";
    }

    @Override
    public DatabaseSyntax syntax() {
        return DatabaseSyntax.MYSQL;
    }

    @Override
    public String engineName() {
        return "mysql";
    }

    @Override
    public String parameters(Map<String, String> parameters) {
        StringBuilder paramURL = new StringBuilder("?");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            paramURL.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return paramURL.substring(0, paramURL.length() - 1);
    }

    @Override
    public ADatabase.ParameterBuilder defaultParameters() {
        return new ADatabase.ParameterBuilder()
                .parameter("useJDBCCompliantTimezoneShift", "true")
                .parameter("useLegacyDatetimeCode", "false")
                .parameter("serverTimezone", "UTC")
                .parameter("autoReconnect", "true");
    }
}
