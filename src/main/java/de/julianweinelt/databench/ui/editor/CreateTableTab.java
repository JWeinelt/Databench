package de.julianweinelt.databench.ui.editor;

import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.ui.BenchUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class CreateTableTab implements IEditorTab {
    @Override
    public JPanel getTabComponent(BenchUI ui, DConnection connection) {


        JPanel editorPanel = new JPanel(new BorderLayout());

        // ===== Toolbar =====
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton createButton = new JButton("💾 Create & Execute");
        JButton cancelButton = new JButton("❌ Cancel");
        toolBar.add(createButton);
        toolBar.add(cancelButton);

        // ===== Table Name Panel =====
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.add(new JLabel("Table name:"));
        JTextField tableNameField = new JTextField(20);
        namePanel.add(tableNameField);

        // ===== Column Table =====
        String[] columnNames = {"Column Name", "Data Type", "Size", "Primary Key", "Not Null", "Auto I"};
        Object[][] data = {
                {"id", "INT", 11, true, true, true},
                {"name", "VARCHAR", 255, false, false, false}
        };

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex >= 3) return Boolean.class;
                return super.getColumnClass(columnIndex);
            }
        };
        JTable columnTable = new JTable(model);
        JScrollPane tableScroll = new JScrollPane(columnTable);

        // ===== SQL Preview =====
        JTextArea sqlPreview = new JTextArea(5, 60);
        sqlPreview.setFont(new Font("Consolas", Font.PLAIN, 13));
        sqlPreview.setEditable(false);
        JScrollPane sqlScroll = new JScrollPane(sqlPreview);

        // ===== SQL Builder =====
        Runnable updateSQL = () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE ").append(tableNameField.getText()).append(" (\n");
            for (int i = 0; i < model.getRowCount(); i++) {
                String colName = model.getValueAt(i, 0).toString();
                String type = model.getValueAt(i, 1).toString();
                String length = model.getValueAt(i, 2).toString();
                boolean pk = (Boolean) model.getValueAt(i, 3);
                boolean nn = (Boolean) model.getValueAt(i, 4);
                boolean ai = (Boolean) model.getValueAt(i, 5);

                sb.append("  ").append(colName).append(" ").append(type);
                if (!length.isEmpty()) sb.append("(").append(length).append(")");
                if (nn) sb.append(" NOT NULL");
                if (ai) sb.append(" AUTO_INCREMENT");
                sb.append(",\n");

                if (pk) sb.append("  PRIMARY KEY (").append(colName).append("),\n");
            }
            int lastComma = sb.lastIndexOf(",");
            if (lastComma != -1) sb.deleteCharAt(lastComma);
            sb.append("\n);");
            sqlPreview.setText(sb.toString());
        };

        // ===== Listener für Änderungen =====
        tableNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateSQL.run(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateSQL.run(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateSQL.run(); }
        });
        model.addTableModelListener(e -> updateSQL.run());
        updateSQL.run();

        createButton.addActionListener(e -> {
            String sql = sqlPreview.getText();
            try {
                if (connection != null) {
                    DConnection.SQLAnswer answer = connection.executeSQL(sql);
                    if (answer.success()) {
                        JOptionPane.showMessageDialog(editorPanel,
                                "Table created successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(editorPanel,
                                "Failed to create table: " + answer.message(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(editorPanel,
                            "No connection available to execute SQL.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editorPanel,
                        "Failed to execute SQL: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // ===== Layout =====
        // ===== NORTH PANEL =====
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
        return "New Table";
    }
}
