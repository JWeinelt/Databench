package de.julianweinelt.databench.dbx.api.ui;

import lombok.Getter;

import javax.swing.*;

@Getter
public abstract class Component {
    private final ComponentType type;
    private String label = "";

    protected Component(ComponentType type) {
        this.type = type;
    }
    public Component label(String label) {
        this.label = label;
        return this;
    }

    public boolean hasLabel() {
        return !label.isBlank();
    }
    public JLabel createLabel() {
        if (!hasLabel()) throw new IllegalStateException("label has not been set");
        return new JLabel(label);
    }

    public abstract JComponent create();
}
