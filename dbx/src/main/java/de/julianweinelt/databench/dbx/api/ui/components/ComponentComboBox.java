package de.julianweinelt.databench.dbx.api.ui.components;

import de.julianweinelt.databench.dbx.api.ui.Component;
import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;
import java.util.HashMap;

public class ComponentComboBox extends Component {
    private final JComboBox<String> comboBox;
    private final HashMap<String, Runnable> options = new HashMap<>();

    public ComponentComboBox() {
        super(ComponentType.COMBOBOX);
        comboBox = new JComboBox<>();
    }

    public ComponentComboBox option(String name, Runnable action) {
        options.put(name, action);
        comboBox.addItem(name);
        return this;
    }

    @Override
    public JComponent create() {
        comboBox.addActionListener(e -> {
            String selected = (String) comboBox.getSelectedItem();
            if (selected == null) return;
            Runnable action = options.getOrDefault(selected, null);
            action.run();
        });
        return comboBox;
    }
}