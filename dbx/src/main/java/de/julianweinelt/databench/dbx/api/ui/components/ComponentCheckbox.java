package de.julianweinelt.databench.dbx.api.ui.components;

import de.julianweinelt.databench.dbx.api.ui.Component;
import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class ComponentCheckbox extends Component<Boolean, ComponentCheckbox> {
    private final JCheckBox checkbox;
    private String checkLabel = "";

    public ComponentCheckbox() {
        super(ComponentType.CHECKBOX);
        checkbox = new JCheckBox();
    }

    public ComponentCheckbox checkLabel(String checkLabel) {
        this.checkLabel = checkLabel;
        return this;
    }

    @Override
    public boolean expandHorizontally() {
        return false;
    }

    @Override
    public ComponentCheckbox initialValue(Object val) {
        if (val instanceof Boolean) {
            checkbox.setSelected((Boolean) val);
        }
        return this;
    }

    @Override
    public Boolean value() {
        return checkbox.isSelected();
    }

    public ComponentCheckbox action(Consumer<ComponentCheckbox> action) {
        checkbox.addActionListener(e -> {
            action.accept(this);
        });
        return this;
    }

    @Override
    public JComponent create() {
        if (!checkLabel.isBlank()) checkbox.setText(checkLabel);
        return checkbox;
    }
}
