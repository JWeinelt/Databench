package de.julianweinelt.databench.ui.plugins;

import javax.swing.*;
import java.awt.*;

public class PluginDetailPanel extends JPanel {

    private final JLabel title;

    public PluginDetailPanel(boolean installedTab) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        JLabel icon = new JLabel();
        icon.setPreferredSize(new Dimension(64, 64));
        icon.setOpaque(true);
        icon.setBackground(new Color(230, 230, 230));

        title = new JLabel("Select a plugin");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JButton action = new JButton(installedTab ? "Update" : "Install");

        header.add(icon, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.add(action, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", new JScrollPane(new JTextArea()));
        tabs.addTab("What's new", new JScrollPane(new JTextArea()));
        tabs.addTab("Reviews", new JScrollPane(new JTextArea()));
        tabs.addTab("Additional Info", new JScrollPane(new JTextArea()));

        add(tabs, BorderLayout.CENTER);
    }

    public void showPlugin(PluginDescriptor plugin) {
        title.setText(plugin.getName() + "  (" + plugin.getVersion() + ")");
    }
}
