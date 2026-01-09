package de.julianweinelt.databench.ui.editor.views;

import lombok.Getter;

import java.util.*;

public class QueryModel {

    private final Map<String, SelectedColumn> selectedColumns = new LinkedHashMap<>();
    @Getter
    private final List<SelectedColumnGridModel> gridModels = new ArrayList<>();

    public void selectColumn(String tableAlias, String tableName, String column) {
        String key = tableAlias + "." + column;
        selectedColumns.putIfAbsent(
                key,
                new SelectedColumn(tableAlias, tableName, column)
        );
        gridModels.forEach(SelectedColumnGridModel::refresh);
    }

    public void unselectColumn(String tableAlias, String column) {
        selectedColumns.remove(tableAlias + "." + column);
        gridModels.forEach(SelectedColumnGridModel::refresh);
    }

    public Collection<SelectedColumn> getSelectedColumns() {
        return selectedColumns.values();
    }
}
