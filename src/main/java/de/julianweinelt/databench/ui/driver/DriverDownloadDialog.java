package de.julianweinelt.databench.ui.driver;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class DriverDownloadDialog extends JDialog {

    private final JComboBox<String> dbTypeBox;
    private final JComboBox<String> versionBox;
    private final JTextArea infoArea;

    private boolean modal;

    private final Map<String, String[]> versions = Map.of(
            "MySQL", new String[]{"9.5.0"},
            "MariaDB", new String[]{"3.5.7", "3.5.0"},
            "MSSQL", new String[]{"13.2.1"}
    );

    public DriverDownloadDialog(Window parent, boolean modal) {
        super(parent, "Download JDBC Driver", ModalityType.APPLICATION_MODAL);
        this.modal = modal;
        setSize(520, 360);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        dbTypeBox = new JComboBox<>(versions.keySet().toArray(new String[0]));
        versionBox = new JComboBox<>();

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Database Type:"), gbc);

        gbc.gridx = 1;
        formPanel.add(dbTypeBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Driver Version:"), gbc);

        gbc.gridx = 1;
        formPanel.add(versionBox, gbc);

        add(formPanel, BorderLayout.NORTH);

        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setText("""
                Select the database type and driver version you want to install.
                
                The driver will be downloaded into the 'drivers' directory and
                loaded automatically on the next application start.
                
                Only official JDBC drivers are provided.
                """);

        JScrollPane scrollPane = new JScrollPane(infoArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Information"));
        add(scrollPane, BorderLayout.CENTER);

        JButton downloadButton = new JButton("Download");
        JButton cancelButton = new JButton("Cancel");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(downloadButton);
        add(buttonPanel, BorderLayout.SOUTH);

        updateVersions();
        dbTypeBox.addActionListener(e -> updateVersions());

        cancelButton.addActionListener(e -> {
            dispose();
            if (modal) new DriverManagerDialog(DriverDownloadDialog.this.getOwner()).setVisible(true);
        });

        downloadButton.addActionListener(e -> {
            String db = (String) dbTypeBox.getSelectedItem();
            String version = (String) versionBox.getSelectedItem();

            new DriverDownloadProgressDialog(this, db, version).setVisible(true);
        });
    }

    private void updateVersions() {
        versionBox.removeAllItems();
        String selectedDb = (String) dbTypeBox.getSelectedItem();
        if (selectedDb != null) {
            for (String v : versions.get(selectedDb)) {
                versionBox.addItem(v);
            }
        }
    }
}
