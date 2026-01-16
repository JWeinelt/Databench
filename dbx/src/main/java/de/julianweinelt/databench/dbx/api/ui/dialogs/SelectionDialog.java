package de.julianweinelt.databench.dbx.api.ui.dialogs;

import lombok.Getter;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SelectionDialog extends JDialog {
    private final NameTableModel tableModel;
    @Getter
    private boolean confirmed = false;

    public SelectionDialog(Frame owner, List<String> names) {
        super(owner, "Select Databases", true);
        this.tableModel = new NameTableModel(names);

        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTable table = new JTable(tableModel);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setMaxWidth(70);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAllBtn = new JButton("Select all");
        JButton deselectAllBtn = new JButton("Deselect all");

        selectAllBtn.addActionListener(e -> tableModel.setAll(true));
        deselectAllBtn.addActionListener(e -> tableModel.setAll(false));

        topPanel.add(selectAllBtn);
        topPanel.add(deselectAllBtn);

        add(topPanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");

        okBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        cancelBtn.addActionListener(e -> dispose());

        bottomPanel.add(cancelBtn);
        bottomPanel.add(okBtn);

        add(bottomPanel, BorderLayout.SOUTH);

        setSize(400, 300);
        setLocationRelativeTo(owner);
    }

    public List<String> getSelectedNames() {
        return tableModel.getSelectedNames();
    }

    private static class NameTableModel extends AbstractTableModel {

        private final List<Row> rows;

        public NameTableModel(List<String> names) {
            rows = new ArrayList<>();
            for (String name : names) {
                rows.add(new Row(false, name));
            }
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? "Selected" : "Name";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Row row = rows.get(rowIndex);
            return columnIndex == 0 ? row.selected : row.name;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                rows.get(rowIndex).selected = (Boolean) aValue;
            }
        }

        public void setAll(boolean value) {
            for (Row row : rows) {
                row.selected = value;
            }
            fireTableDataChanged();
        }

        public List<String> getSelectedNames() {
            List<String> result = new ArrayList<>();
            for (Row row : rows) {
                if (row.selected) {
                    result.add(row.name);
                }
            }
            return result;
        }

        private static class Row {
            boolean selected;
            String name;

            Row(boolean selected, String name) {
                this.selected = selected;
                this.name = name;
            }
        }
    }
}