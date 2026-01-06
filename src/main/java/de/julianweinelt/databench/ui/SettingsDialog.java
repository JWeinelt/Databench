package de.julianweinelt.databench.ui;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import de.julianweinelt.databench.data.Configuration;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class SettingsDialog extends JDialog {

    public SettingsDialog(Frame owner) {
        super(owner, "Preferences", true);
        setSize(520, 420);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General", createGeneralPanel());
        tabs.addTab("Appearance", createAppearancePanel());
        tabs.addTab("Updates", createUpdatePanel());
        tabs.addTab("Advanced", createAdvancedPanel());

        add(tabs, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        panel.add(new JLabel("Language:"), c);

        c.gridx = 1;
        c.weightx = 1;
        panel.add(new JComboBox<>(LanguageManager.instance().getFriendlyNames().toArray()), c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weightx = 0;
        panel.add(new JCheckBox("Open last project on startup"), c);

        c.gridy++;
        panel.add(new JCheckBox("Show start page on startup"), c);

        return panel;
    }


    private JPanel createAppearancePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = baseConstraints();

        panel.add(new JLabel("Theme:"), c);
        c.gridx++;
        JComboBox<String> themes = new JComboBox<>(new String[]{"Light", "Dark", "Darcula", "Dark (MacOS)", "Light (MacOS)", "IntelliJ"});
        themes.addActionListener(e -> {
            String selected = (String) themes.getSelectedItem();
            if (selected == null) return;
            FlatLaf laf = switch (selected) {
                case "Light" -> new FlatLightLaf();
                case "Dark" -> new FlatIntelliJLaf();
                case "Darcula" -> new FlatDarculaLaf();
                case "Dark (MacOS)" -> new FlatMacDarkLaf();
                case "Light (MacOS)" -> new FlatMacLightLaf();
                case "IntelliJ" -> new FlatIntelliJLaf();
                default -> new FlatDarkLaf();
            };
            ThemeSwitcher.switchTheme(laf);
            Configuration.getConfiguration().setSelectedTheme(selected); // optional speichern
        });
        panel.add(themes, c);

        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Font size:"), c);
        c.gridx++;
        panel.add(new JSpinner(new SpinnerNumberModel(13, 10, 20, 1)), c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        panel.add(new JCheckBox("Use Animations"), c);

        return panel;
    }

    private JPanel createUpdatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = baseConstraints();

        panel.add(new JCheckBox("Automatically search for updates"), c);

        c.gridy++;
        panel.add(new JLabel("Update Channel:"), c);
        c.gridy++;
        panel.add(new JComboBox<>(new String[]{"stable", "nightly", "beta"}), c);

        return panel;
    }

    private JPanel createAdvancedPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = baseConstraints();

        panel.add(new JCheckBox("Automatically send error protocols"), c);

        c.gridy++;
        panel.add(new JCheckBox("Use Debug logging"), c);

        c.gridx++;
        panel.add(new JButton("Reset preferences"), c);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton apply = new JButton("Apply");
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");

        ok.addActionListener(e -> {
            // saveSettings();
            dispose();
        });

        apply.addActionListener(e -> {
            // saveSettings();
        });

        cancel.addActionListener(e -> dispose());

        panel.add(apply);
        panel.add(ok);
        panel.add(cancel);

        return panel;
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 6, 6, 6);
        return c;
    }
}
