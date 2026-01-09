package de.julianweinelt.databench.ui;

import javax.swing.*;
import java.awt.*;

public class LoadingPopup {

    private JDialog dialog;

    public LoadingPopup(JFrame parent, String message) {
        dialog = new JDialog(parent, true);
        dialog.setSize(300, 120);
        dialog.setLocationRelativeTo(parent);

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2, true));
        panel.setBackground(new Color(50, 50, 50));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(message);
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setPreferredSize(new Dimension(250, 20));
        progressBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));

        panel.add(label);
        panel.add(progressBar);

        dialog.add(panel);
    }

    public void showPopup() {
        SwingUtilities.invokeLater(() -> dialog.setVisible(true));
    }

    public void closePopup() {
        SwingUtilities.invokeLater(() -> dialog.dispose());
    }
}
