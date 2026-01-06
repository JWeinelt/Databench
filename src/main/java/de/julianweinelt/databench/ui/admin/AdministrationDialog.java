package de.julianweinelt.databench.ui.admin;

import javax.swing.*;
import java.awt.*;

public class AdministrationDialog extends JDialog {

    private final JTabbedPane tabs = new JTabbedPane();

    private final GeneralSettingsPanel generalPanel;
    private final UserManagementPanel userPanel;
    private final PerformancePanel performancePanel;
    private final ProcessListPanel processPanel;

    public AdministrationDialog(Window owner) {
        super(owner, "Administration", ModalityType.APPLICATION_MODAL);

        generalPanel = new GeneralSettingsPanel();
        userPanel = new UserManagementPanel();
        performancePanel = new PerformancePanel();
        processPanel = new ProcessListPanel();

        tabs.addTab("General", generalPanel);
        tabs.addTab("Users", userPanel);
        tabs.addTab("Performance", performancePanel);
        tabs.addTab("Processes", processPanel);

        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c instanceof Refreshable r) {
                r.refresh();
            }
        });

        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        add(createBottomBar(), BorderLayout.SOUTH);

        setPreferredSize(new Dimension(1100, 700));
        pack();
        setLocationRelativeTo(owner);
    }

    private JComponent createBottomBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c instanceof Refreshable r) {
                r.refresh();
            }
        });

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());

        panel.add(refresh);
        panel.add(close);
        return panel;
    }
}
