package de.julianweinelt.databench.dbx.api.ui.components;

import de.julianweinelt.databench.dbx.api.ui.Component;
import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;
import java.awt.*;

public class ComponentLabel extends Component<Void, ComponentLabel> {
    private final JLabel label;
    private final boolean header;

    public ComponentLabel(boolean header) {
        super(ComponentType.LABEL);
        this.header = header;
        label = new JLabel();
    }

    public ComponentLabel text(String text) {
        label.setText(text);
        return this;
    }
    public ComponentLabel font(String font, int size) {
        label.setFont(new Font(font, Font.PLAIN, size));
        return this;
    }
    public ComponentLabel font(Font font) {
        label.setFont(font);
        return this;
    }
    public ComponentLabel fontSize(float size) {
        label.getFont().deriveFont(size);
        return this;
    }
    public ComponentLabel bold() {
        label.getFont().deriveFont(Font.BOLD);
        return this;
    }
    public ComponentLabel italic() {
        label.getFont().deriveFont(Font.ITALIC);
        return this;
    }

    @Override
    public boolean expandHorizontally() {
        return header;
    }

    @Override
    public ComponentLabel initialValue(Object val) {
        return this;
    }

    @Override
    public Void value() {
        return null;
    }

    @Override
    public JComponent create() {
        return label;
    }
}
