package de.julianweinelt.datacat.ui.importdiag;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DatabaseMappingDialog extends JDialog {

    private static final String CREATE_NEW = "<Neu erstellen>";

    @Getter
    private boolean confirmed = false;

    private final Map<String, MappingRow> rows = new LinkedHashMap<>();

    public DatabaseMappingDialog(
            Window owner,
            List<String> archiveDatabases,
            List<String> existingDatabases
    ) {
        super(owner, "Database Mapping", ModalityType.APPLICATION_MODAL);

        setLayout(new BorderLayout(8, 8));

        add(createContent(
                archiveDatabases,
                existingDatabases
        ), BorderLayout.CENTER);

        add(createButtons(), BorderLayout.SOUTH);

        setPreferredSize(new Dimension(800, 500));
        pack();
        setLocationRelativeTo(owner);
    }

    private JScrollPane createContent(
            List<String> archiveDatabases,
            List<String> existingDatabases
    ) {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Archive Database"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(new JLabel("Target Database"), gbc);

        gbc.gridx = 2;
        gbc.weightx = 1;
        panel.add(new JLabel("New Name"), gbc);

        int rowIndex = 1;

        for (String archiveDatabase : archiveDatabases) {

            gbc.gridy = rowIndex;

            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(new JLabel(archiveDatabase), gbc);

            JComboBox<DatabaseMap> targetBox = new JComboBox<>();

            targetBox.addItem(DatabaseMap.ignore());
            targetBox.addItem(DatabaseMap.createNew());

            for (String database : existingDatabases) {
                //targetBox.addItem(new DatabaseMap());
            }

            if (existingDatabases.contains(archiveDatabase)) {
                targetBox.setSelectedItem(archiveDatabase);
            }

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(targetBox, gbc);

            JTextField newNameField =
                    new JTextField(archiveDatabase);

            boolean createNew =
                    CREATE_NEW.equals(
                            targetBox.getSelectedItem()
                    );

            newNameField.setEnabled(createNew);

            targetBox.addActionListener(event -> {
                boolean selected =
                        CREATE_NEW.equals(
                                targetBox.getSelectedItem()
                        );

                newNameField.setEnabled(selected);
            });

            gbc.gridx = 2;
            gbc.weightx = 1;
            panel.add(newNameField, gbc);

            rows.put(
                    archiveDatabase,
                    new MappingRow(
                            archiveDatabase,
                            targetBox,
                            newNameField
                    )
            );

            rowIndex++;
        }

        return new JScrollPane(panel);
    }

    private JPanel createButtons() {

        JButton cancelButton = new JButton("Cancel");

        cancelButton.addActionListener(event -> {
            confirmed = false;
            dispose();
        });

        JButton okButton = new JButton("OK");

        okButton.addActionListener(event -> {

            for (MappingRow row : rows.values()) {

                if (row.isCreateNew()) {

                    String value =
                            row.newNameField().getText().trim();

                    if (value.isEmpty()) {

                        JOptionPane.showMessageDialog(
                                this,
                                "Please enter a name for all new databases.",
                                "Validation",
                                JOptionPane.WARNING_MESSAGE
                        );

                        return;
                    }
                }
            }

            confirmed = true;
            dispose();
        });

        JPanel panel =
                new JPanel(new FlowLayout(
                        FlowLayout.RIGHT
                ));

        panel.add(cancelButton);
        panel.add(okButton);

        return panel;
    }

    public Map<String, String> getMappings() {

        Map<String, String> result =
                new LinkedHashMap<>();

        for (MappingRow row : rows.values()) {

            String target;

            if (row.isCreateNew()) {
                target = row.newNameField()
                        .getText()
                        .trim();
                log.info("Target new: {}", target);
            } else {
                target = ((DatabaseMap) row.targetBox().getSelectedItem()).targetDatabase();
                log.info("Target: {}", target);
            }

            result.put(
                    row.archiveDatabase(),
                    target
            );
        }

        return result;
    }

    private record MappingRow(
            String archiveDatabase,
            JComboBox<DatabaseMap> targetBox,
            JTextField newNameField
    ) {

        boolean isCreateNew() {
            return CREATE_NEW.equals(
                    targetBox.getSelectedItem()
            );
        }
    }
}