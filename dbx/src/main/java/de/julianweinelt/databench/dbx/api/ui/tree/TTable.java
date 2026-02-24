package de.julianweinelt.databench.dbx.api.ui.tree;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class TTable {
    private final UUID uniqueID;
    private final String database;
    private String name;

    private final List<TColumn> columns = new ArrayList<>();
    private final List<TableRelation> relations = new ArrayList<>();

    public TTable(UUID uniqueID, String database, String name) {
        this.uniqueID = uniqueID;
        this.database = database;
        this.name = name;
    }
}