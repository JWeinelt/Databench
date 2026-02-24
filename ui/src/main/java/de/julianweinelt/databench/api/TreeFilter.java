package de.julianweinelt.databench.api;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TreeFilter {
    private final List<Filter> filters = new ArrayList<>();

    public List<Filter> getFilter(String database, FilterContext context) {
        return filters.stream().filter(f -> f.database().equals(database) && f.context().equals(context)).toList();
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }
    public void clearFilter(FilterContext context, String database) {
        filters.removeIf(f -> f.database().equals(database) && f.context() == context);
    }


    public List<FilterRow> showFilterDialog(Window parent) {
        JDialog dialog = new JDialog(parent, "Edit filters", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(10,10));

        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));

        List<RowComponents> rows = new ArrayList<>();

        JButton addButton = new JButton("+ Filter");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        Runnable addRow = () -> {
            if (rows.size() >= 5) return;

            JPanel row = new JPanel(new GridLayout(1,4,5,5));

            JComboBox<FilterType> typeBox = new JComboBox<>(FilterType.values());
            JTextField valueField = new JTextField();
            JComboBox<LogicalOperator> opBox = new JComboBox<>(LogicalOperator.values());
            JButton remove = new JButton("X");

            row.add(typeBox);
            row.add(valueField);
            row.add(opBox);
            row.add(remove);

            RowComponents comp = new RowComponents(row, typeBox, valueField, opBox);
            rows.add(comp);
            tablePanel.add(row);

            remove.addActionListener(e -> {
                rows.remove(comp);
                tablePanel.remove(row);
                tablePanel.revalidate();
                tablePanel.repaint();
                addButton.setEnabled(rows.size() < 5);
            });

            addButton.setEnabled(rows.size() < 5);

            tablePanel.revalidate();
            tablePanel.repaint();
        };

        addButton.addActionListener(e -> addRow.run());

        addRow.run();

        final List<FilterRow>[] result = new List[]{null};

        okButton.addActionListener(e -> {
            List<FilterRow> filters = new ArrayList<>();

            for (RowComponents r : rows) {
                String text = r.value().getText().trim();
                if (!text.isEmpty()) {
                    filters.add(new FilterRow(
                            (FilterType) r.type().getSelectedItem(),
                            text,
                            (LogicalOperator) r.operator().getSelectedItem()
                    ));
                }
            }

            result[0] = filters;
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(addButton);
        bottom.add(cancelButton);
        bottom.add(okButton);

        dialog.add(new JScrollPane(tablePanel), BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.setSize(600,300);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return result[0];
    }

    private record RowComponents(
            JPanel panel,
            JComboBox<FilterType> type,
            JTextField value,
            JComboBox<LogicalOperator> operator
    ) {}


    public record Filter(String database, FilterContext context, FilterType type, String value) {}

    public enum FilterContext {
        TABLES,
        VIEWS,
        PROCEDURES,
        FUNCTIONS
    }

    public enum FilterType {
        CONTAINS,
        BEGINS_WITH,
        ENDS_WITH,
        EQUAL,
        NOT_EQUAL
    }

    public enum LogicalOperator {
        AND("Und"),
        OR("Oder"),
        XOR("Ex-Oder");

        private final String label;

        LogicalOperator(String label) {
            this.label = label;
        }

        public String toString() {
            return label;
        }
    }
    public record FilterRow(FilterType type, String value, LogicalOperator operator) {}
}