package de.julianweinelt.databench.ui.admin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class UserManagementPanel extends JPanel implements Refreshable {

    private final JTable userTable;
    private final JTabbedPane detailTabs;

    public UserManagementPanel() {
        setLayout(new BorderLayout(8, 8));

        userTable = new JTable(new DefaultTableModel(
                new Object[]{"User", "Host"}, 0
        ));
        userTable.setRowHeight(24);

        JScrollPane left = new JScrollPane(userTable);

        detailTabs = new JTabbedPane();
        detailTabs.addTab("Basic Info", new JPanel());
        detailTabs.addTab("Roles", new JPanel());
        detailTabs.addTab("Schema Privileges", new JPanel());

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                left,
                detailTabs
        );
        split.setDividerLocation(300);

        add(split, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                DefaultTableModel model = (DefaultTableModel) userTable.getModel();
                model.setRowCount(0);

                publish(new Object[]{"root", "localhost"});
                publish(new Object[]{"app_user", "%"});

                return null;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                DefaultTableModel model = (DefaultTableModel) userTable.getModel();
                for (Object[] row : chunks) {
                    model.addRow(row);
                }
            }
        };
        worker.execute();
    }
}
