package de.julianweinelt.databench.launcher.setup.wizard;

import de.julianweinelt.databench.launcher.setup.SetupState;
import de.julianweinelt.databench.launcher.setup.WizardPage;

import javax.swing.*;
import java.awt.*;

public class ThemePage implements WizardPage {

    private final SetupState state;
    private JPanel panel;
    private JRadioButton darkButton;
    private JRadioButton lightButton;
    private JCheckBox systemCheck;

    public ThemePage(SetupState state) {
        this.state = state;
        initUI();
    }

    private void initUI() {
        panel = new JPanel(new BorderLayout(10, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        panel.setBackground(UIManager.getColor("Panel.background"));

        JLabel title = new JLabel("Select Theme");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        panel.add(title, BorderLayout.NORTH);

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBackground(panel.getBackground());

        darkButton = new JRadioButton("Dark");
        lightButton = new JRadioButton("Light");

        darkButton.setFont(darkButton.getFont().deriveFont(16f));
        lightButton.setFont(lightButton.getFont().deriveFont(16f));

        ButtonGroup group = new ButtonGroup();
        group.add(darkButton);
        group.add(lightButton);

        // Default: abhängig vom System
        boolean systemDark = UIManager.getBoolean("Component.shouldUseDarkTheme");
        if (systemDark) {
            darkButton.setSelected(true);
        } else {
            lightButton.setSelected(true);
        }

        systemCheck = new JCheckBox("Follow system theme", true);
        systemCheck.setBackground(panel.getBackground());
        systemCheck.addActionListener(e -> state.followSystemTheme = systemCheck.isSelected());

        options.add(darkButton);
        options.add(lightButton);
        options.add(Box.createVerticalStrut(10));
        options.add(systemCheck);

        panel.add(options, BorderLayout.CENTER);
    }

    @Override
    public String getId() {
        return "theme";
    }

    @Override
    public JComponent getView() {
        return panel;
    }

    @Override
    public boolean canGoNext() {
        // Theme speichern
        if (darkButton.isSelected()) state.theme = "dark";
        else state.theme = "light";
        return true;
    }
}
