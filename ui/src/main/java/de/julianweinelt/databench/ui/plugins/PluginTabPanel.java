package de.julianweinelt.databench.ui.plugins;

import javax.swing.*;
import java.awt.*;

public class PluginTabPanel extends JPanel {

    private final boolean installedTab;
    private final PluginDetailPanel detailPanel;

    public PluginTabPanel(boolean installedTab) {
        this.installedTab = installedTab;
        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0);
        splitPane.setDividerLocation(380);
        splitPane.setEnabled(false);

        detailPanel = new PluginDetailPanel(installedTab);

        PluginListPanel listPanel = new PluginListPanel(installedTab, detailPanel::showPlugin);

        splitPane.setLeftComponent(listPanel);
        splitPane.setRightComponent(detailPanel);

        add(splitPane, BorderLayout.CENTER);
    }
}
