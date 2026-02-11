package de.julianweinelt.databench.dbx.database;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


@Slf4j
public class DatabaseReturnSelection {
    private final List<Value> values;

    public DatabaseReturnSelection(List<Value> values) {
        this.values = values;
    }
    public DatabaseReturnSelection() {
        values = new ArrayList<>();
    }

    public boolean hasValue(String name) {
        for (Value v : values) if (v.name.equals(name)) return true;
        return false;
    }
    public Value value(String name) {
        for (Value v : values) if (v.name.equals(name)) return v;
        return null;
    }

    public static boolean validate(DatabaseReturnSelection schema, DatabaseReturnSchemaTemplate template) {
        int errors = 0;
        for (String field : template.getFields().keySet()) {
            boolean required = template.getFields().get(field);
            if (!schema.hasValue(field) && required) errors++;
        }
        if (errors != 0) log.warn("Validation found {} errors.", errors);
        return errors == 0;
    }
    public static List<DatabaseReturnSelection> fromResultSet(ResultSet set) {
        List<DatabaseReturnSelection> rows = new ArrayList<>();

        //TODO
        return rows;
    }


    public static class DatabaseReturnSchemaTemplate {
        @Getter
        private final HashMap<String, Boolean> fields = new HashMap<>();

        public DatabaseReturnSchemaTemplate field(String name) {
            return field(name, true);
        }

        public DatabaseReturnSchemaTemplate field(String name, boolean required) {
            fields.put(name, required);
            return this;
        }
    }

    public record Value(String name, Object value) {
        public String asString() {
                return (String) value;
            }

        public boolean asBoolean() {
                return (boolean) value;
            }

        public int asInt() {
                return (int) value;
            }

        public long asLong() {
                return (long) value;
            }

        public char asChar() {
                return (char) value;
            }

        public Date asDate(boolean compatible) {
                if (!compatible) return (Date) value;
                else return Date.from(Instant.ofEpochSecond(asLong()));
            }

        public <T> T as(Class<T> type) {
                return type.cast(value);
            }
        }
}
