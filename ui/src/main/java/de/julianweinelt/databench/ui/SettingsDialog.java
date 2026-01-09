package de.julianweinelt.databench.ui;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import de.julianweinelt.databench.data.ConfigManager;
import de.julianweinelt.databench.data.Configuration;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
        tabs.addTab("Keyboard Shortcuts", createShortcutPanel());

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
        JComboBox<Object> comp = new JComboBox<>(LanguageManager.instance().getFriendlyNames().toArray());
        comp.addActionListener(e -> {
            String selected = (String) comp.getSelectedItem();

            String langID = LanguageManager.instance().fromFriendlyName(selected);
            log.info("Selected language: {}", langID);
            Configuration.getConfiguration().setLocale(langID);
            ConfigManager.getInstance().saveConfig();
        });
        panel.add(comp, c);

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
        JComboBox<String> themes = new JComboBox<>(new String[]{"Dark", "Light", "Darcula", "Dark (MacOS)", "Light (MacOS)", "IntelliJ"});
        themes.addActionListener(e -> {
            String selected = (String) themes.getSelectedItem();
            if (selected == null) return;
            FlatLaf laf = switch (selected) {
                case "Light" -> new FlatLightLaf();
                case "Dark" -> new FlatDarkLaf();
                case "Darcula" -> new FlatDarculaLaf();
                case "Dark (MacOS)" -> new FlatMacDarkLaf();
                case "Light (MacOS)" -> new FlatMacLightLaf();
                case "IntelliJ" -> new FlatIntelliJLaf();
                default -> new FlatDarkLaf();
            };
            ThemeSwitcher.switchTheme(laf);
            Configuration.getConfiguration().setSelectedTheme(selected);
            ConfigManager.getInstance().saveConfig();
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

    private JPanel createShortcutPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        DefaultListModel<ShortcutAction> model = new DefaultListModel<>();
        for (ShortcutAction action : ShortcutAction.values()) {
            model.addElement(action);
        }

        JList<ShortcutAction> actionList = new JList<>(model);
        actionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        actionList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.getDisplayName());
            label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });

        panel.add(new JScrollPane(actionList), BorderLayout.WEST);

        panel.add(createShortcutDetailPanel(actionList), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createShortcutDetailPanel(JList<ShortcutAction> list) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = baseConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel actionName = new JLabel();
        JTextField shortcutField = new JTextField();
        shortcutField.setEditable(false);

        JButton change = new JButton("Change…");
        JButton reset = new JButton("Reset");

        c.gridwidth = 2;
        panel.add(actionName, c);

        c.gridy++;
        panel.add(shortcutField, c);

        c.gridy++;
        c.gridwidth = 1;
        panel.add(change, c);
        c.gridx++;
        panel.add(reset, c);

        list.addListSelectionListener(e -> {
            ShortcutAction action = list.getSelectedValue();
            if (action == null) return;

            actionName.setText(action.getDisplayName());

            KeyStroke ks = Configuration.getConfiguration()
                    .getShortcut(action.name(), action.getDefaultKey());

            shortcutField.setText(KeyStrokeUtils.toString(ks));
        });

        change.addActionListener(e -> {
            ShortcutAction action = list.getSelectedValue();
            if (action == null) return;

            KeyStroke ks = captureKeyStroke(panel);
            if (ks != null) {
                Configuration.getConfiguration()
                        .setShortcut(action.name(), ks);
                ConfigManager.getInstance().saveConfig();
                shortcutField.setText(KeyStrokeUtils.toString(ks));
            }
        });

        reset.addActionListener(e -> {
            ShortcutAction action = list.getSelectedValue();
            if (action == null) return;

            Configuration.getConfiguration().removeShortcut(action.name());
            shortcutField.setText(KeyStrokeUtils.toString(action.getDefaultKey()));
        });

        return panel;
    }

    private KeyStroke captureKeyStroke(Component parent) {
        final JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(parent),
                "Press shortcut",
                ModalityType.APPLICATION_MODAL
        );

        JLabel label = new JLabel("Press the desired key combination…", SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dialog.add(label);

        dialog.setSize(360, 140);
        dialog.setLocationRelativeTo(parent);

        final KeyStroke[] result = new KeyStroke[1];
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        KeyEventDispatcher dispatcher = e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED)
                return false;

            int code = e.getKeyCode();

            if (isModifierKey(code))
                return true;

            int modifiers = e.getModifiersEx();

            result[0] = KeyStroke.getKeyStroke(code, modifiers);

            label.setText(
                    KeyEvent.getModifiersExText(modifiers) + " + " +
                            KeyEvent.getKeyText(code)
            );

            dialog.dispose();
            return true;
        };

        kfm.addKeyEventDispatcher(dispatcher);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                kfm.removeKeyEventDispatcher(dispatcher);
            }
        });

        dialog.setVisible(true);
        return result[0];
    }

    private boolean isModifierKey(int keyCode) {
        return keyCode == KeyEvent.VK_SHIFT
                || keyCode == KeyEvent.VK_CONTROL
                || keyCode == KeyEvent.VK_ALT
                || keyCode == KeyEvent.VK_META
                || keyCode == KeyEvent.VK_ALT_GRAPH;
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
