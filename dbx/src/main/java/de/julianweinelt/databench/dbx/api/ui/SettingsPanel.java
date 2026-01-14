package de.julianweinelt.databench.dbx.api.ui;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel {
    private final JPanel panel;
    private final GridBagConstraints c;
    private String title;

    public SettingsPanel(String title) {
        panel = new JPanel(new GridBagLayout());
        this.title = title;
        panel.setName(title);
        c = baseConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        return c;
    }

    public void add(Component component) {
        c.gridy++;
        if (component.hasLabel()) {
            c.gridx = 0;
            panel.add(component.createLabel());
            c.gridx = 1;
            panel.add(component.create());
        } else {
            c.gridx = 0;
            panel.add(component.create());
        }
    }
    public void setTitle(String title) {
        this.title = title;
        panel.setName(title);
    }
    public JPanel createPanel() {
        return panel;
    }
    public String title() {
        return title;
    }
}
