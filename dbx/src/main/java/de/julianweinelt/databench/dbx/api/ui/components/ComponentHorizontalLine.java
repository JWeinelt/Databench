package de.julianweinelt.databench.dbx.api.ui.components;

import de.julianweinelt.databench.dbx.api.ui.Component;
import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;

public class ComponentHorizontalLine extends Component<Void, ComponentHorizontalLine> {

    private final JSeparator separator;

    public ComponentHorizontalLine() {
        super(ComponentType.SEPARATOR);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
    }

    @Override
    public boolean hasLabel() {
        return false;
    }

    @Override
    public boolean expandHorizontally() {
        return true;
    }

    @Override
    public ComponentHorizontalLine initialValue(Object val) {
        return this;
    }

    @Override
    public Void value() {
        return null;
    }

    @Override
    public JComponent create() {
        return separator;
    }
}