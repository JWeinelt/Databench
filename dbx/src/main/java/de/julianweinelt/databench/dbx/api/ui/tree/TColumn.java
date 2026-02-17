package de.julianweinelt.databench.dbx.api.ui.tree;


public class TColumn {
    private String name;
    private TDataType dataType;
    private boolean notNull;
    private Object defaultValue;
    private boolean primaryKey;
    private boolean unique;
    //TODO: CHECK
    private boolean autoIncrement;
    private boolean unsigned;
    private String comment;
}
