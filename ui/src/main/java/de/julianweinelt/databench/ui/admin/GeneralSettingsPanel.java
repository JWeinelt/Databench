package de.julianweinelt.databench.ui.admin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class GeneralSettingsPanel extends JPanel implements Refreshable {

    private final JTable table;

    public GeneralSettingsPanel() {
        setLayout(new BorderLayout(8, 8));

        table = new JTable(new DefaultTableModel(
                new Object[]{"Setting", "Value"}, 0
        ));

        table.setFillsViewportHeight(true);
        table.setRowHeight(24);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);

                // Beispiel – später via Service
                publish(new Object[]{"Server Version", "8.0.36"});
                publish(new Object[]{"Uptime", "2 days 4 hours"});
                publish(new Object[]{"Default Charset", "utf8mb4"});
                publish(new Object[]{"Time Zone", "SYSTEM"});
                publish(new Object[]{"Max Connections", "151"});

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
