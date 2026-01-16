package de.julianweinelt.databench.dbx.api.ui.components;

import de.julianweinelt.databench.dbx.api.ui.Component;
import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;
import java.util.function.Consumer;

public class ComponentSpinner extends Component<Integer, ComponentSpinner> {
    private final JSpinner spinner;

    public ComponentSpinner() {
        super(ComponentType.NUMBER);
        spinner = new JSpinner();
    }

    @Override
    public boolean expandHorizontally() {
        return false;
    }

    @Override
    public ComponentSpinner initialValue(Object val) {
        if (val == null) return this;
        if (val instanceof Integer) {
            spinner.setValue(val);
        }
        return this;
    }

    public ComponentSpinner action(Consumer<Integer> action) {
        action.accept((Integer) spinner.getValue());
        return this;
    }

    @Override
    public Integer value() {
        return (Integer) spinner.getValue();
    }

    @Override
    public JComponent create() {
        return spinner;
    }
}
