package de.julianweinelt.databench.dbx.database;

import java.util.Map;

public interface DatabaseMetaData {
    String jdbcString();
    String jdbcURL();
    DatabaseSyntax syntax();
    String engineName();
    String parameters(Map<String, String> parameters);
    ADatabase.ParameterBuilder defaultParameters();
    int defaultPort();
}
