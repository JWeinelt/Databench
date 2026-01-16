package de.julianweinelt.databench.dbx.api.ui.components;

import de.julianweinelt.databench.dbx.api.ui.Component;
import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class ComponentButton extends Component<Void, ComponentButton> {
    private String text;
    private final JButton button;

    public ComponentButton(String text) {
        super(ComponentType.BUTTON);
        this.text = text;
        button = new JButton(text);
    }

    public ComponentButton text(String text) {
        this.text = text;
        button.setText(text);
        return this;
    }

    @Override
    public boolean expandHorizontally() {
        return false;
    }

    @Override
    public ComponentButton initialValue(Object _val) {
        return this;
    }

    @Override
    public Void value() {
        return null;
    }

    public ComponentButton action(Consumer<ActionEvent> action) {
        button.addActionListener(action::accept);
        return this;
    }

    @Override
    public JComponent create() {
        return button;
    }
}
