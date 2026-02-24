package de.julianweinelt.databench.ui.editor;

import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.ui.BenchUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateTableTab implements IEditorTab {
    private final UUID id = UUID.randomUUID();

    private TableDefinition table;
    private TableDefinition originalTable;

    private Object[][] tableData;

    private final DConnection connection;
    private boolean existingTable = false;
    private String tableName = null;
    private String database = null;

    public CreateTableTab(DConnection connection) {
        this.connection = connection;
    }

    public CreateTableTab ofRealTable(String database, String tableName) {
        this.tableName = tableName;
        this.database = database;

        this.table = connection.getTableDefinition(database, tableName);
        this.originalTable = deepCopy(this.table);

        this.tableData = toTableData(this.table.getColumns());
        this.existingTable = true;
        return this;
    }

    public CreateTableTab newTable(String database) {
        this.table = new TableDefinition();
        this.table.setTableName("");
        this.database = database;

        table.addColumn(new TableColumn("id", "INT", 11, true, true, true));
        table.addColumn(new TableColumn("name", "VARCHAR", 255, false, false, false));

        this.tableData = toTableData(table.getColumns());
        this.existingTable = false;
        return this;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public JPanel getTabComponent(BenchUI ui, DConnection connection) {

        JPanel editorPanel = new JPanel(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton applyButton = new JButton("💾 " + (existingTable ? "Apply" : "Create"));
        JButton revertButton = new JButton("❌ Revert");
        JButton newCol = new JButton("+");
        JButton remCol = new JButton("-");

        toolBar.add(applyButton);
        toolBar.add(revertButton);
        toolBar.add(newCol);
        toolBar.add(remCol);

        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.add(new JLabel("Table name:"));

        JTextField tableNameField = new JTextField(20);
        if (tableName != null) tableNameField.setText(tableName);
        namePanel.add(tableNameField);

        String[] columnNames = {
                "Column Name", "Data Type", "Size",
                "Primary Key", "Not Null", "Auto I"
        };

        DefaultTableModel model = new DefaultTableModel(tableData, columnNames) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex >= 3 ? Boolean.class : String.class;
            }
        };

        JTable columnTable = new JTable(model);
        JScrollPane tableScroll = new JScrollPane(columnTable);

        JTextArea sqlPreview = new JTextArea(6, 60);
        sqlPreview.setFont(new Font("Consolas", Font.PLAIN, 13));
        sqlPreview.setEditable(false);

        JScrollPane sqlScroll = new JScrollPane(sqlPreview);

        Runnable updateSQL = () -> {
            table.setTableName(tableNameField.getText());
            table.getColumns().clear();
            table.getColumns().addAll(fromTableModel(model));

            String txt;
            if (existingTable) txt = SQLBuilder.buildAlter(originalTable, table);
            else txt = SQLBuilder.buildCreateOrAlter(table, existingTable);

            sqlPreview.setText(txt);
        };

        tableNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateSQL.run(); }
            public void removeUpdate(DocumentEvent e) { updateSQL.run(); }
            public void changedUpdate(DocumentEvent e) { updateSQL.run(); }
        });

        model.addTableModelListener(e -> updateSQL.run());
        updateSQL.run();

        newCol.addActionListener(e -> {
            table.addColumn(new TableColumn("", "", null, false, false, false));
            updateSQL.run();
            model.addRow(new Object[]{
                    "",
                    "",
                    "",
                    false,
                    false,
                    false
            });
        });
        remCol.addActionListener(e -> {
            int[] selectedRows = columnTable.getSelectedRows();

            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(
                        editorPanel,
                        "Please select at least one column to remove.",
                        "No selection",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                model.removeRow(selectedRows[i]);
            }
        });

        // ===== APPLY =====
        applyButton.addActionListener(e -> {
            try {
                connection.executeSQL("USE " + database + ";");
                DConnection.SQLAnswer answer =
                        connection.executeSQL(sqlPreview.getText());

                if (answer.success()) {
                    JOptionPane.showMessageDialog(editorPanel,
                            "Operation successful",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(editorPanel,
                            answer.message(),
                            "SQL Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editorPanel,
                        ex.getMessage(),
                        "Exception",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // ===== LAYOUT =====
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(toolBar);
        topPanel.add(namePanel);

        editorPanel.add(topPanel, BorderLayout.NORTH);
        editorPanel.add(tableScroll, BorderLayout.CENTER);
        editorPanel.add(sqlScroll, BorderLayout.SOUTH);

        return editorPanel;
    }

    @Override
    public String getTitle() {
        return tableName == null ? "New Table" : tableName;
    }

    private TableDefinition deepCopy(TableDefinition src) {
        TableDefinition copy = new TableDefinition();
        copy.setTableName(src.getTableName());
        for (TableColumn c : src.getColumns()) {
            copy.addColumn(new TableColumn(
                    c.getName(), c.getType(), c.getSize(),
                    c.isPrimaryKey(), c.isNotNull(), c.isAutoIncrement()
            ));
        }
        return copy;
    }

    private Object[][] toTableData(List<TableColumn> columns) {
        Object[][] data = new Object[columns.size()][6];
        for (int i = 0; i < columns.size(); i++) {
            TableColumn c = columns.get(i);
            data[i] = new Object[]{
                    c.getName(),
                    c.getType(),
                    c.getSize() != null ? c.getSize() : "",
                    c.isPrimaryKey(),
                    c.isNotNull(),
                    c.isAutoIncrement()
            };
        }
        return data;
    }

    private List<TableColumn> fromTableModel(DefaultTableModel model) {
        List<TableColumn> columns = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            columns.add(new TableColumn(
                    model.getValueAt(i, 0).toString(),
                    model.getValueAt(i, 1).toString(),
                    model.getValueAt(i, 2).toString().isEmpty()
                            ? null
                            : Integer.parseInt(model.getValueAt(i, 2).toString()),
                    (Boolean) model.getValueAt(i, 3),
                    (Boolean) model.getValueAt(i, 4),
                    (Boolean) model.getValueAt(i, 5)
            ));
        }
        return columns;
    }
}
