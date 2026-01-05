package de.julianweinelt.databench.ui.editor;

import java.util.Objects;

public class TableColumn {

    private String name;
    private String type;
    private Integer size;
    private boolean primaryKey;
    private boolean notNull;
    private boolean autoIncrement;

    public TableColumn(String name, String type, Integer size,
                       boolean primaryKey, boolean notNull, boolean autoIncrement) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.primaryKey = primaryKey;
        this.notNull = notNull;
        this.autoIncrement = autoIncrement;
    }

    public TableColumn() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public boolean isPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }

    public boolean isNotNull() { return notNull; }
    public void setNotNull(boolean notNull) { this.notNull = notNull; }

    public boolean isAutoIncrement() { return autoIncrement; }
    public void setAutoIncrement(boolean autoIncrement) { this.autoIncrement = autoIncrement; }

    public boolean equalsDefinition(TableColumn other) {
        if (other == null) return false;

        return Objects.equals(type, other.type)
                && Objects.equals(size, other.size)
                && notNull == other.notNull
                && autoIncrement == other.autoIncrement;
    }

}
