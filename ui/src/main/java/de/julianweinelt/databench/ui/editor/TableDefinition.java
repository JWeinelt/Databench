package de.julianweinelt.databench.ui.editor;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TableDefinition {
    @Setter
    private String tableName;
    private final List<TableColumn> columns = new ArrayList<>();


    public void addColumn(TableColumn column) {
        columns.add(column);
    }
}
