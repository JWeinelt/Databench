package de.julianweinelt.tobbql.parser;

public enum QueryResultType {
        SUCCESS,
        RESULT_SET,
        FAILED,
        ALREADY_EXISTS,
        SAFE_MODE_ENABLED,
        UNKNOWN_DB,
        UNKNOWN_TABLE,
        UNKNOWN_ACTION,
        EMPTY
    }