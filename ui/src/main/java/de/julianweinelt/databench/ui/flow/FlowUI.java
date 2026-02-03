package de.julianweinelt.databench.ui.flow;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class FlowUI {
    public JPanel createFlowLoginPanel() {
        JPanel root = new JPanel(new GridBagLayout());

        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createTitledBorder("Flow Login"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        JLabel lblUser = new JLabel("Username");
        gbc.gridx = 0;
        gbc.gridy = 0;
        card.add(lblUser, gbc);

        JTextField txtUser = new JTextField(20);
        gbc.gridy = 1;
        card.add(txtUser, gbc);

        JLabel lblPass = new JLabel("Password");
        gbc.gridy = 2;
        card.add(lblPass, gbc);

        JPasswordField txtPass = new JPasswordField(20);
        gbc.gridy = 3;
        card.add(txtPass, gbc);

        JCheckBox remember = new JCheckBox("Remember credentials");
        gbc.gridy = 4;
        card.add(remember, gbc);

        JButton loginBtn = new JButton("Login");
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(loginBtn, gbc);

        loginBtn.addActionListener(e -> {
            String username = txtUser.getText();
            char[] password = txtPass.getPassword();
            boolean save = remember.isSelected();

            if (username.isBlank()) {
                JOptionPane.showMessageDialog(card, "Please enter a username.");
                return;
            }

            if (password.length == 0) {
                JOptionPane.showMessageDialog(card, "Please enter a password.");
                return;
            }

            loginBtn.setEnabled(false);

            SwingUtilities.invokeLater(() -> {
                try {
                    boolean success = attemptFlowLogin(username, password);

                    if (success) {
                        if (save) {
                            saveCredentials(username, password);
                        }
                        showFlowDashboard(root);
                    } else {
                        JOptionPane.showMessageDialog(card, "Login failed.");
                        loginBtn.setEnabled(true);
                    }
                } finally {
                    Arrays.fill(password, '\0');
                }
            });
        });

        root.add(card);
        return root;
    }

    private void showFlowDashboard(JPanel root) {

    }

    private void saveCredentials(String username, char[] password) {

    }

    private boolean attemptFlowLogin(String username, char[] password) {
        return false;
    }
}
