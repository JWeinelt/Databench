package de.julianweinelt.tobbql.api;

import com.google.gson.JsonObject;
import de.julianweinelt.tobbql.parser.Type;

public class Query {
    public static String buildGetDatabases() {
        JsonObject o = new JsonObject();
        o.addProperty("type", Type.GET_DATABASES.name());
        return o.toString();
    }

    public static String buildGetTables(String database) {
        JsonObject o = new JsonObject();
        o.addProperty("type", Type.GET_TABLES.name());
        o.addProperty("database", database);
        return o.toString();
    }

    //TODO: Add more query builders (e.g., for executing SQL queries, getting table schema, etc.
}
