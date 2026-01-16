package de.julianweinelt.databench.ui.plugins;

import javax.swing.*;
import java.awt.*;

public class PluginCardPanel extends JPanel {

    public PluginCardPanel(PluginDescriptor plugin) {
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JLabel icon = new JLabel();
        icon.setPreferredSize(new Dimension(64, 64));
        icon.setOpaque(true);
        icon.setBackground(new Color(240, 240, 240));
        add(icon, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel name = new JLabel(plugin.getName());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));

        JLabel version = new JLabel("Version " + plugin.getVersion());
        version.setFont(version.getFont().deriveFont(11f));
        version.setForeground(Color.GRAY);

        JLabel desc = new JLabel("<html><body style='width:260px'>" + plugin.getDescription() + "</body></html>");
        desc.setFont(desc.getFont().deriveFont(12f));

        textPanel.add(name);
        textPanel.add(version);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(desc);

        add(textPanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));

        if (plugin.isInstalled()) {
            JCheckBox enabled = new JCheckBox("Enabled", plugin.isEnabled());
            actionPanel.add(enabled);

            if (plugin.isUpdateAvailable()) {
                JButton update = new JButton("Update");
                actionPanel.add(Box.createVerticalStrut(5));
                actionPanel.add(update);
            }
        } else {
            JButton install = new JButton("Install");
            install.setContentAreaFilled(false);
            install.setBorder(BorderFactory.createLineBorder(new Color(0, 160, 0)));
            install.setForeground(new Color(0, 160, 0));
            actionPanel.add(install);
        }

        add(actionPanel, BorderLayout.EAST);
    }
}
