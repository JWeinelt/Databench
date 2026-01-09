package de.julianweinelt.databench.ui.editor.views;

import lombok.extern.slf4j.Slf4j;

import javax.swing.table.AbstractTableModel;
import java.util.List;

@Slf4j
public class ColumnSelectTableModel extends AbstractTableModel {

    private final List<String> columns;
    private final boolean[] selected;
    private final QueryModel queryModel;
    private final TableNode owner;

    public ColumnSelectTableModel(
            List<String> columns,
            QueryModel queryModel,
            TableNode owner
    ) {
        this.columns = columns;
        this.selected = new boolean[columns.size()];
        this.queryModel = queryModel;
        this.owner = owner;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (col != 0) return;

        selected[row] = (Boolean) value;
        String column = columns.get(row);

        if (selected[row]) {
            queryModel.selectColumn(
                    owner.getAlias(),
                    owner.getTableName(),
                    column
            );
            log.info("Selected column {}", column);
        } else {
            queryModel.unselectColumn(owner.getAlias(), column);
            log.info("Unselected column {}", column);
        }

        fireTableRowsUpdated(row, row);
    }

    @Override
    public int getRowCount() {
        return columns.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? "Selected" : "Column";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return columnIndex == 0
                ? selected[rowIndex]
                : columns.get(rowIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    public boolean isSelected(int row) {
        return selected[row];
    }
}
