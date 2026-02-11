package de.julianweinelt.databench.ui.plugins;

import javax.swing.*;
import java.awt.*;

public class PluginDialog extends JDialog {

    public PluginDialog(Frame owner) {
        super(owner, "Plugins", true);
        setSize(1000, 650);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Marketplace", new PluginTabPanel(false));
        tabbedPane.addTab("Installed", new PluginTabPanel(true));

        add(tabbedPane, BorderLayout.CENTER);
    }
}