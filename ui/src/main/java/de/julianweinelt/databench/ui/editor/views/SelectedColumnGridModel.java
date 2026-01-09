package de.julianweinelt.databench.ui.editor.views;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class SelectedColumnGridModel extends AbstractTableModel {

    private final QueryModel queryModel;
    private final List<SelectedColumn> rows = new ArrayList<>();

    public SelectedColumnGridModel(QueryModel queryModel) {
        this.queryModel = queryModel;
        refresh();
    }

    public void refresh() {
        rows.clear();
        rows.addAll(queryModel.getSelectedColumns());
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public String getColumnName(int col) {
        return switch (col) {
            case 0 -> "Column";
            case 1 -> "Alias";
            case 2 -> "Table";
            case 3 -> "Output";
            case 4 -> "Sort";
            case 5 -> "Filter";
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return col == 3 ? Boolean.class : String.class;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (rows.size() <= row) return (col == 3) ? false : "";
        SelectedColumn c = rows.get(row);
        return switch (col) {
            case 0 -> c.columnName;
            case 1 -> c.alias;
            case 2 -> c.tableAlias;
            case 3 -> c.output;
            case 4 -> c.sort;
            case 5 -> c.filter;
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col >= 1;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        SelectedColumn c = rows.get(row);
        switch (col) {
            case 1 -> c.alias = value.toString();
            case 3 -> c.output = (Boolean) value;
            case 4 -> c.sort = value.toString();
            case 5 -> c.filter = value.toString();
        }
        fireTableRowsUpdated(row, row);
    }
}
