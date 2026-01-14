package de.julianweinelt.databench.dbx.api.ui.components;

import de.julianweinelt.databench.dbx.api.ui.Component;
import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;

public class ComponentTextField extends Component {
    private String placeholder = "";

    protected ComponentTextField() {
        super(ComponentType.TEXT);
    }

    public ComponentTextField placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Override
    public JComponent create() {
        JTextField field = new JTextField();
        if (!placeholder.isBlank()) field.setText(placeholder);
        return field;
    }
}
