package de.julianweinelt.databench.launcher.setup.wizard;

import de.julianweinelt.databench.launcher.setup.SetupState;
import de.julianweinelt.databench.launcher.setup.WizardPage;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DriverPage implements WizardPage {

    private final SetupState state;
    private JPanel panel;

    // Struktur: Vendor -> Checkbox + Version ComboBox
    private final Map<String, JCheckBox> vendorCheckboxes = new HashMap<>();
    private final Map<String, JComboBox<String>> vendorVersionBoxes = new HashMap<>();

    // Beispiel-Versionslisten
    private final Map<String, String[]> availableVersions = Map.of(
            "MSSQL", new String[]{"2017", "2019", "2022"},
            "MySQL", new String[]{"8.0.33", "8.0.32", "5.7"},
            "PostgreSQL", new String[]{"15", "14", "13"},
            "MariaDB", new String[]{"11", "10.11", "10.10"}
    );

    public DriverPage(SetupState state) {
        this.state = state;
        initUI();
    }

    private void initUI() {
        panel = new JPanel(new BorderLayout(10, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        panel.setBackground(UIManager.getColor("Panel.background"));

        JLabel title = new JLabel("Select Database Drivers");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        panel.add(title, BorderLayout.NORTH);

        JPanel driverPanel = new JPanel();
        driverPanel.setLayout(new BoxLayout(driverPanel, BoxLayout.Y_AXIS));
        driverPanel.setBackground(panel.getBackground());

        for (String vendor : availableVersions.keySet()) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            row.setBackground(panel.getBackground());

            JCheckBox cb = new JCheckBox(vendor);
            cb.setFont(cb.getFont().deriveFont(16f));
            cb.setSelected(false);

            JComboBox<String> versions = new JComboBox<>(availableVersions.get(vendor));
            versions.setFont(versions.getFont().deriveFont(14f));
            versions.setEnabled(false);

            cb.addActionListener(e -> versions.setEnabled(cb.isSelected()));

            vendorCheckboxes.put(vendor, cb);
            vendorVersionBoxes.put(vendor, versions);

            row.add(cb);
            row.add(new JLabel("Version:"));
            row.add(versions);

            driverPanel.add(row);
        }

        JScrollPane scrollPane = new JScrollPane(driverPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public String getId() {
        return "drivers";
    }

    @Override
    public JComponent getView() {
        return panel;
    }

    @Override
    public boolean canGoNext() {
        // mindestens ein Treiber muss ausgewählt sein
        boolean valid = vendorCheckboxes.values().stream().anyMatch(JCheckBox::isSelected);
        if (valid) {
            state.selectedDrivers.clear();
            for (String vendor : vendorCheckboxes.keySet()) {
                if (vendorCheckboxes.get(vendor).isSelected()) {
                    String version = (String) vendorVersionBoxes.get(vendor).getSelectedItem();
                    state.selectedDrivers.add(vendor + ":" + version);
                }
            }
        }
        return valid;
    }
}
