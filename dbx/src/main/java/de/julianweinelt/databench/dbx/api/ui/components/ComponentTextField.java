package de.julianweinelt.databench.dbx.api.ui.components;

import de.julianweinelt.databench.dbx.api.ui.Component;
import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;

public class ComponentTextField extends Component<String, ComponentTextField> {
    private String placeholder = "";
    protected JTextField field;

    protected ComponentTextField() {
        super(ComponentType.TEXT);
        field = new JTextField();
    }

    public ComponentTextField placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Override
    public ComponentTextField initialValue(Object val) {
        if (val instanceof String) {
            field.setText((String) val);
        }
        return this;
    }

    @Override
    public String value() {
        return field.getText();
    }

    @Override
    public boolean expandHorizontally() {
        return true;
    }

    @Override
    public JComponent create() {
        if (!placeholder.isBlank()) field.setText(placeholder);
        return field;
    }
}
