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
            c.anchor = GridBagConstraints.WEST;
            panel.add(component.createLabel(), c.clone());

            c.gridx = 1;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = component.expandHorizontally() ? 1.0 : 0.0;
            c.fill = component.expandHorizontally() ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
            panel.add(component.create(), c.clone());

            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
        } else {
            c.gridx = 0;
            c.gridwidth = 2;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = component.expandHorizontally() ? 1.0 : 0.0;
            c.fill = component.expandHorizontally() ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
            panel.add(component.create(), c.clone());

            c.gridwidth = 1;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
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

    public void finish() {
        GridBagConstraints spacer = baseConstraints();
        spacer.gridx = 0;
        spacer.gridy = c.gridy + 1;
        spacer.weighty = 1.0;
        spacer.fill = GridBagConstraints.VERTICAL;

        panel.add(Box.createVerticalGlue(), spacer);
    }
}
