package de.julianweinelt.datacat.dbx.api.ui;

import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

import javax.swing.*;

@Getter
public abstract class Component<T, C> {
    private final ComponentType type;
    private String label = "";

    protected Component(ComponentType type) {
        this.type = type;
    }

    /**
     * Sets the label to be displayed for the component.<br>
     * Set to an empty String to disable the label.
     * @param label A String with the content of the label
     * @return The modified {@link Component} object
     */
    public C label(String label) {
        this.label = label;
        return (C) this;
    }

    /**
     * Checks if a label has been set for this component
     * @return <code>true</code> if the components has a non-empty label, otherwise <code>false</code>
     */
    public boolean hasLabel() {
        return !label.isBlank();
    }

    /**
     * Creates the label object for display
     * @return A new {@link JLabel} containing the text of the component
     */
    @ApiStatus.Internal
    public JLabel createLabel() {
        if (!hasLabel()) throw new IllegalStateException("label has not been set");
        return new JLabel(label);
    }

    /**
     * Defines if the component should be expanded to the window's width size in the component's parent screen.
     * <p></p>
     * Example: a horizontal line for splitting content
     * @return <code>true</code> if it should be expanded, otherwise <code>false</code>
     */
    public abstract boolean expandHorizontally();

    /**
     * For setting an initial value that should be displayed before any of the user's input.<br>
     * It should always return the instance of the {@link Component}.
     * <p></p>
     * <b>Implementation example:</b>
     * <pre>{@code
     * protected JTextField textField;
     *
     * @Override
     * public ComponentTextField initialValue(Object value) {
     *     if (value instanceof String) {
     *         textField.setText((String) value);
     *     }
     *     return this;
     * }
     * }</pre>
     * @param val The value to set.
     * @return The {@link Component}'s instance.
     */
    public abstract C initialValue(Object val);

    /**
     * Should return the value of the Component's Swing Object. e.g., if implementing a text field, it should return the
     * entered value of the text field.
     * @return The entered value of this component.
     */
    public abstract T value();

    /**
     * Should return the internal Swing object.
     * @return An object extending {@link JComponent}
     */
    public abstract JComponent create();
}
