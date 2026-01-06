package de.julianweinelt.databench.ui.admin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ProcessListPanel extends JPanel implements Refreshable {

    private final JTable table;

    public ProcessListPanel() {
        setLayout(new BorderLayout(8, 8));

        table = new JTable(new DefaultTableModel(
                new Object[]{"Id", "User", "Host", "DB", "Command", "Time", "State", "Info"}, 0
        ));
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);

                publish(new Object[]{12, "app_user", "10.0.0.5", "db1", "Query", 3, "executing", "SELECT * FROM orders"});
                publish(new Object[]{15, "root", "localhost", null, "Sleep", 120, "", null});

                return null;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                for (Object[] row : chunks) {
                    model.addRow(row);
                }
            }
        };
        worker.execute();
    }
}
