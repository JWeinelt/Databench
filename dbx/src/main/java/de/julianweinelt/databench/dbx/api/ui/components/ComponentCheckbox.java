package de.julianweinelt.databench.dbx.api.ui.components;

import de.julianweinelt.databench.dbx.api.ui.Component;
import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class ComponentCheckbox extends Component {
    private final JCheckBox checkbox;

    public ComponentCheckbox() {
        super(ComponentType.CHECKBOX);
        checkbox = new JCheckBox();
    }

    public ComponentCheckbox action(Consumer<ActionEvent> action) {
        checkbox.addActionListener(action::accept);
        return this;
    }

    @Override
    public JComponent create() {
        if (hasLabel()) checkbox.setText(getLabel());
        return checkbox;
    }
}
