package de.julianweinelt.datacat.dbx.api.ui.dialogs;

import de.julianweinelt.datacat.dbx.api.ui.Component;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.List;

import static de.julianweinelt.datacat.dbx.util.LanguageManager.translate;

public class Dialog {
    private JFrame parent;

    private final String title;
    private final JPanel panel;
    private final GridBagConstraints c;

    private final JPanel actionPanel;

    private final List<Component> components = new ArrayList<>();
    private final HashMap<String, DialogAction> actions = new HashMap<>();

    public Dialog(String title) {
        this.title = title;
        panel = new JPanel(new GridBagLayout());
        panel.setName(title);
        c = baseConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;

        actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
    }

    public void action(String name, DialogAction action) {
        actions.put(name, action);
    }


    /**
     * Add a {@link de.julianweinelt.datacat.dbx.api.ui.Component} to the panel, moving to the next free row.
     * @param component The component to add
     */
    public void add(Component component) {
        components.add(component);
    }


    private void addComponent(Component component) {
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

    private GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        return c;
    }


    public JPanel createPanel() {
        return panel;
    }
    public String title() {
        return title;
    }

    public void finish() {
        components.forEach(this::addComponent);
    }

    private void buildActions() {
        for (String s : actions.keySet()) {
            DialogAction a = actions.get(s);
            JButton button = new JButton((a.translateName() ? translate(a.buttonName()) : a.buttonName()));
            button.addActionListener(_e -> {
                a.call();
            });
        }
    }
}