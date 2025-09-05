package de.julianweinelt.tobbql.parser;

public enum Type {
        GET_DATA(true, false),
        GET_TABLES(true, false),
        INSERT_DATA(false, true),
        UPDATE_DATA(false, true),
        DELETE_DATA(false, true),
        CREATE_TABLE(false, true),
        DELETE_TABLE(false, true),
        EDIT_TABLE(false, true),
        CLEAR_TABLE(false, true),
        GRANT_PERMISSION(false, true),
        CREATE_DATABASE(false, true),
        DELETE_DATABASE(false, true),

        CREATE_USER(false, true),
        DEACTIVATE_USER(false, true),
        EDIT_USER(false, true),
        DELETE_USER(false, true),

        GET_DATABASES(true, false),

        CREATE_ROLE(false, true),
        EDIT_ROLE(false, true),
        DELETE_ROLE(false, true),

        UNKNOWN(false, false);

        public final boolean returnsResultSet;
        public final boolean changesRows;

        Type(boolean returnsResultSet, boolean changesRows) {
            this.returnsResultSet = returnsResultSet;
            this.changesRows = changesRows;
        }
    }