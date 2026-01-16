package de.julianweinelt.databench.dbx.api.ui.components;


import de.julianweinelt.databench.dbx.api.ui.ComponentType;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ComponentColorPicker extends
        de.julianweinelt.databench.dbx.api.ui.Component<Color, ComponentColorPicker> {

    private final JButton button;
    private final JPanel panel;
    private final JButton resetButton;

    private Color defaultColor = new Color(78, 80, 82);

    private Color color;

    public ComponentColorPicker() {
        super(ComponentType.COLOR);

        button = new JButton();
        button.setPreferredSize(new Dimension(32, 20));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        button.setContentAreaFilled(true);

        panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        panel.setOpaque(false);

        resetButton = new JButton("⟲");
        resetButton.setFocusable(false);
        resetButton.setMargin(new Insets(0, 4, 0, 4));
        resetButton.addActionListener(e -> {
            if (defaultColor != null) {
                setColor(defaultColor);
            }
        });

        panel.add(button);
        panel.add(resetButton);
    }
    public ComponentColorPicker defaultColor(Color defaultColor) {
        this.defaultColor = defaultColor;
        return this;
    }
    public ComponentColorPicker resetAction(Runnable action) {
        resetButton.addActionListener(e -> {
            action.run();
        });
        return this;
    }

    private void setColor(Color color) {
        this.color = color;
        button.setBackground(color);
        button.setForeground(getReadableTextColor(color));
    }

    public ComponentColorPicker action(Consumer<Color> action) {
        button.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(
                    button,
                    "Select Color",
                    color != null ? color : Color.WHITE
            );

            if (chosen != null) {
                setColor(chosen);
                action.accept(chosen);
            }
        });
        return this;
    }

    @Override
    public boolean expandHorizontally() {
        return false;
    }

    @Override
    public ComponentColorPicker initialValue(Object val) {
        if (val instanceof Color c) {
            setColor(c);
        }
        return this;
    }

    @Override
    public Color value() {
        return color;
    }

    @Override
    public JComponent create() {
        return panel;
    }

    private Color getReadableTextColor(Color bg) {
        double luminance =
                (0.299 * bg.getRed()
               + 0.587 * bg.getGreen()
               + 0.114 * bg.getBlue()) / 255;

        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }
}
