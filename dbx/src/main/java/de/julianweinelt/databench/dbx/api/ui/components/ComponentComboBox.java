package de.julianweinelt.databench.dbx.api.ui.components;

import de.julianweinelt.databench.dbx.api.ui.Component;
import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;
import java.util.HashMap;
import java.util.function.Consumer;

public class ComponentComboBox extends Component<String, ComponentComboBox> {
    private final JComboBox<String> comboBox;
    private final HashMap<String, Runnable> options = new HashMap<>();
    private final HashMap<String, String> displayLabels = new HashMap<>();

    public ComponentComboBox() {
        super(ComponentType.COMBOBOX);
        comboBox = new JComboBox<>();
    }

    public ComponentComboBox option(String name, String label, Runnable action) {
        options.put(name, action);
        comboBox.addItem(label);
        displayLabels.put(name, label);
        return this;
    }

    public ComponentComboBox option(String name, String label) {
        comboBox.addItem(label);
        displayLabels.put(name, label);
        return this;
    }

    @Override
    public String value() {
        if (comboBox.getSelectedItem() == null) return "";
        String val = comboBox.getSelectedItem().toString();
        for (String s : displayLabels.keySet()) {
            if (displayLabels.get(s).equals(val)) {
                return s;
            }
        }
        return "";
    }
    public String selectedLabel() {
        return (comboBox.getSelectedItem() == null) ? "" : comboBox.getSelectedItem().toString();
    }
    public int index() {
        return comboBox.getSelectedIndex();
    }

    public ComponentComboBox action(Consumer<String> action) {
        comboBox.addActionListener(i -> action.accept(value()));
        return this;
    }

    @Override
    public ComponentComboBox initialValue(Object val) {
        if (val instanceof String) {
            comboBox.setSelectedItem(val);
        }
        return this;
    }

    @Override
    public boolean expandHorizontally() {
        return false;
    }

    @Override
    public JComponent create() {
        comboBox.addActionListener(e -> {
            String selected = (String) comboBox.getSelectedItem();
            if (selected == null) return;
            Runnable action = options.getOrDefault(selected, null);
            if (action == null) return;
            action.run();
        });
        return comboBox;
    }
}