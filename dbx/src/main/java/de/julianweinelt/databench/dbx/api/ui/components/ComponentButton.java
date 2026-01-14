package de.julianweinelt.databench.dbx.api.ui.components;

import de.julianweinelt.databench.dbx.api.ui.Component;
import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class ComponentButton extends Component {
    private String text;
    private final JButton button;

    protected ComponentButton(String text) {
        super(ComponentType.BUTTON);
        this.text = text;
        button = new JButton(text);
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
