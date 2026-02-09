package de.julianweinelt.databench.ui.flow;

import de.julianweinelt.databench.ui.flow.window.FlowWindow;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class FlowUI {
    private JPanel root;

    private final FlowClient client;

    public FlowUI(FlowClient client) {
        this.client = client;
    }


    public JPanel createFlowLoginPanel() {
        root = new JPanel(new GridBagLayout());

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

            try {
                AtomicReference<String> error = new AtomicReference<>();
                boolean success = attemptFlowLogin(username, password, error::set).join();

                if (success) {
                    if (save) {
                        saveCredentials(username, password);
                    }
                    showFlowDashboard(root);
                } else {
                    JOptionPane.showMessageDialog(card, "Login failed with message:\n" + error.get());
                    loginBtn.setEnabled(true);
                }
            } finally {
                Arrays.fill(password, '\0');
            }
        });

        root.add(card);
        return root;
    }

    private void showFlowDashboard(JPanel root) {
        root.removeAll();
        root.setLayout(new BorderLayout(12, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Flow Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        header.add(title, BorderLayout.WEST);

        JButton logout = new JButton("Logout");
        header.add(logout, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel lblUser = new JLabel("User:");
        content.add(lblUser, gbc);

        gbc.gridx = 1;
        JLabel valUser = new JLabel(client.getToken() != null ? "Authenticated" : "Unknown");
        content.add(valUser, gbc);

        gbc.gridx = 0;
        gbc.gridy++;

        JLabel lblServer = new JLabel("Server:");
        content.add(lblServer, gbc);

        gbc.gridx = 1;
        JLabel valServer = new JLabel(client.getEnabled().get() ? "Online" : "Offline");
        valServer.setForeground(client.getEnabled().get() ? new Color(0, 128, 0) : Color.RED);
        content.add(valServer, gbc);

        gbc.gridx = 0;
        gbc.gridy++;

        JButton refresh = new JButton("Refresh Status");
        content.add(refresh, gbc);

        gbc.gridx = 0;
        gbc.gridy++;

        JButton openWindow = new JButton("Open Flow");
        openWindow.addActionListener(e -> new FlowWindow().open());
        content.add(openWindow, gbc);

        root.add(content, BorderLayout.CENTER);

        logout.addActionListener(e -> {
            client.getEnabled().set(false);
            root.removeAll();
            root.add(createFlowLoginPanel());
            root.revalidate();
            root.repaint();
        });

        openWindow.addActionListener(e -> {
            openWindow.setEnabled(false);
            CompletableFuture.runAsync(() -> {
                boolean enabled = client.getEnabled().get();
                SwingUtilities.invokeLater(() -> {
                    valServer.setText(enabled ? "Online" : "Offline");
                    valServer.setForeground(enabled ? new Color(0, 128, 0) : Color.RED);
                    openWindow.setEnabled(true);
                });
            });
        });

        root.revalidate();
        root.repaint();
    }

    private void saveCredentials(String username, char[] password) {

    }

    private CompletableFuture<Boolean> attemptFlowLogin(String username, char[] password, Consumer<String> onError) {
        return client.login(username, new String(password), onError);
    }
}