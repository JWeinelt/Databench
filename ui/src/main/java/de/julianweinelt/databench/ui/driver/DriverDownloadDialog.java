package de.julianweinelt.databench.ui.driver;

import de.julianweinelt.databench.dbx.api.DbxAPI;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Map;

import static de.julianweinelt.databench.ui.LanguageManager.translate;

public class DriverDownloadDialog extends JDialog {

    private final JComboBox<String> dbTypeBox;
    private final JComboBox<String> versionBox;
    private final JTextArea infoArea;

    private boolean modal;

    private final String[] mySQLVersions = {
            "9.5.0",
            "9.4.0","9.3.0","9.2.0","9.1.0","9.0.0",

            "8.4.0","8.3.0","8.2.0","8.1.0","8.0.33","8.0.32","8.0.31","8.0.30","8.0.29","8.0.28","8.0.27","8.0.26",
            "8.0.25","8.0.24","8.0.23","8.0.22","8.0.21","8.0.20","8.0.19","8.0.18","8.0.17","8.0.16","8.0.15","8.0.14",
            "8.0.13","8.0.12","8.0.11",

            "8.0.9 rc","8.0.8 dmr","8.0.7 dmr",

            "6.0.6 m5","6.0.5 m4","6.0.4 m3","6.0.3 m2","6.0.2 m1",

            "5.1.49","5.1.48","5.1.47","5.1.46","5.1.45","5.1.44","5.1.43","5.1.42","5.1.41","5.1.40","5.1.39",
            "5.1.38","5.1.37","5.1.36","5.1.35","5.1.34","5.1.33","5.1.32","5.1.31","5.1.30","5.1.29","5.1.28","5.1.27",
            "5.1.26","5.1.25","5.1.24","5.1.23","5.1.22","5.1.21","5.1.20","5.1.19","5.1.18","5.1.17","5.1.16","5.1.15",
            "5.1.14","5.1.13","5.1.12","5.1.11","5.1.10","5.1.9","5.1.8","5.1.7","5.1.6","5.1.5","5.1.4","5.1.3 rc",
            "5.1.2 beta","5.1.1 alpha","5.1.0 alpha",

            "5.0.8","5.0.7","5.0.6","5.0.5","5.0.4","5.0.3","5.0.2 beta","5.0.1 beta","5.0.0 beta",

            "3.2.0 alpha","3.1.14","3.1.13","3.1.12","3.1.11","3.1.10","3.1.9","3.1.8a","3.1.8","3.1.7","3.1.6",
            "3.1.5 gamma","3.1.4 beta","3.1.3 beta","3.1.2 alpha","3.1.1 alpha","3.1.0 alpha",
            "3.0.17","3.0.16","3.0.15","3.0.14","3.0.13","3.0.12","3.0.11","3.0.10","3.0.9","3.0.8","3.0.7","3.0.6",
            "3.0.5 gamma","3.0.4 gamma","3.0.3 beta","3.0.2 beta","3.0.1 beta","3.0.0 beta",

            "2.0.14"
    };

    private final Map<String, String[]> versions = Map.of(
            "mysql", mySQLVersions,
            "mariadb", new String[]{"3.5.7", "3.4.0", "2.7.13"},
            "mssql", new String[]{"13.2.1", "12.10.2", "12.8.2"},
            "postgresql", new String[]{"42.7.8", "42.7.7", "42.7.6", "42.7.5", "42.7.4", "42.7.3", "42.7.1"}
    );

    private String fromInternalDBName(String name) {
        return switch (name) {
            case "mysql" -> "MySQL";
            case "mariadb" -> "MariaDB";
            case "mssql" -> "Microsoft SQL Server";
            case "postgresql" -> "Postgre SQL";
            default -> name;
        };
    }
    private String toInternalDBName(String name) {
        return switch (name) {
            case "MySQL" -> "mysql";
            case "MariaDB" -> "mariadb";
            case "Microsoft SQL Server" -> "mssql";
            case "Postgre SQL" -> "postgresql";
            default -> name;
        };
    }

    public DriverDownloadDialog(Window parent, boolean modal) {
        super(parent, translate("dialog.driver.download.title"), ModalityType.APPLICATION_MODAL);
        this.modal = modal;
        setSize(520, 360);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        String[] dbEngines = new String[versions.size()];
        int i = 0;
        for (String s : versions.keySet()) {
            dbEngines[i++] = fromInternalDBName(s);
        }

        dbTypeBox = new JComboBox<>(dbEngines);
        versionBox = new JComboBox<>();

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel(translate("dialog.driver.download.dbtype")), gbc);

        gbc.gridx = 1;
        formPanel.add(dbTypeBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel(translate("dialog.driver.download.version")), gbc);

        gbc.gridx = 1;
        formPanel.add(versionBox, gbc);

        add(formPanel, BorderLayout.NORTH);

        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setText(translate("dialog.driver.download.description"));

        JScrollPane scrollPane = new JScrollPane(infoArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Information"));
        add(scrollPane, BorderLayout.CENTER);

        JButton downloadButton = new JButton(translate("dialog.driver.download.button.download"));
        JButton cancelButton = new JButton(translate("dialog.driver.download.button.close"));

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
            if (db == null) return;
            String version = (String) versionBox.getSelectedItem();

            DriverDownloadWrapper.DriverDownload driverDownload = DriverDownloadWrapper.getForDB(toInternalDBName(db), version);
            if (driverDownload == null) return;

            new DriverDownloadProgressDialog(this, driverDownload.url(), DbxAPI.driversFolder(),
                    driverDownload, db, version).setVisible(true);
        });
    }

    private void updateVersions() {
        versionBox.removeAllItems();
        String selectedDb = (String) dbTypeBox.getSelectedItem();
        if (selectedDb != null) {
            for (String v : versions.get(toInternalDBName(selectedDb))) {
                versionBox.addItem(v);
            }
        }
    }
}