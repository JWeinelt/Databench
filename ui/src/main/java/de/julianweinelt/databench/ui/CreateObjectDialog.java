package de.julianweinelt.databench.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class CreateObjectDialog extends JDialog {

    public enum CreateType {
        SCHEMA("Schema"),
        TABLE("Table"),
        VIEW("View"),
        PROCEDURE("Procedure"),
        FUNCTION("Function");

        private final String label;

        CreateType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private CreateType selectedType;

    public CreateObjectDialog(Frame owner) {
        super(owner, "Create New Database Object", true);
        setSize(350, 300);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        DefaultListModel<CreateType> model = new DefaultListModel<>();
        for (CreateType type : CreateType.values()) {
            model.addElement(type);
        }

        JList<CreateType> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setFont(list.getFont().deriveFont(14f));

        JScrollPane scrollPane = new JScrollPane(list);

        JLabel hintLabel = new JLabel(
                "Use ↑ ↓ to select, Enter to confirm, Esc to cancel"
        );
        hintLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        hintLabel.setForeground(Color.GRAY);

        JButton createButton = new JButton("Create");
        JButton cancelButton = new JButton("Cancel");

        createButton.addActionListener(e -> {
            selectedType = list.getSelectedValue();
            dispose();
        });

        cancelButton.addActionListener(e -> {
            selectedType = null;
            dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);

        add(scrollPane, BorderLayout.CENTER);
        add(hintLabel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);

        // ENTER = Create
        getRootPane().setDefaultButton(createButton);

        // ESC = Cancel
        getRootPane().registerKeyboardAction(
                e -> {
                    selectedType = null;
                    dispose();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    public CreateType getSelectedType() {
        return selectedType;
    }
}
