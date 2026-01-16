package de.julianweinelt.databench.ui;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import de.julianweinelt.databench.data.ConfigManager;
import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.dbx.api.ui.SettingsPanel;
import de.julianweinelt.databench.dbx.api.ui.UIService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

@Slf4j
public class SettingsDialog extends JDialog {
    private boolean saved = true;
    private JButton applyButton;
    private final java.util.List<Runnable> toSaveRuns = new ArrayList<>();

    public SettingsDialog(Frame owner) {
        super(owner, "Preferences", true);
        setFont(Configuration.getConfiguration().getEditorFontObject());
        setSize(600, 420);
        setLocationRelativeTo(owner);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();

        for (SettingsPanel p : UIService.instance().getSettingsPanels()) {
            tabs.addTab(p.title(), wrapPanel(p));
        }
        tabs.addTab("Keyboard Shortcuts", createShortcutPanel());

        mainPanel.add(tabs, BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JScrollPane wrapPanel(SettingsPanel settingsPanel) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.add(settingsPanel.createPanel());

        JScrollPane scroll = new JScrollPane(wrapper);

        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBlockIncrement(64);
        scroll.setBorder(null);
        return scroll;
    }

    private JPanel createShortcutPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        DefaultListModel<ShortcutAction> model = new DefaultListModel<>();
        for (ShortcutAction action : ShortcutAction.values()) {
            model.addElement(action);
        }

        JList<ShortcutAction> actionList = new JList<>(model);
        actionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        actionList.setLayoutOrientation(JList.VERTICAL);
        actionList.setVisibleRowCount(-1);
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

        panel.add(new JScrollPane(actionList), BorderLayout.LINE_START);
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

        applyButton = new JButton("Apply");
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");

        ok.addActionListener(e -> {
            applyButton.doClick();
            dispose();
        });
        applyButton.addActionListener(e -> {
            toSaveRuns.forEach(Runnable::run);
            toSaveRuns.clear();
            applyButton.setEnabled(false);
            ConfigManager.getInstance().saveConfig();
        });
        cancel.addActionListener(e -> {
            if (!saved)  {
                int val = JOptionPane.showConfirmDialog(SettingsDialog.this, "You have unsaved " +
                        "changes. If you close this window, they will get lost. Continue?", "Unsaved changes", JOptionPane.OK_CANCEL_OPTION);
                if (val == JOptionPane.OK_OPTION) {
                    dispose();
                    toSaveRuns.clear();
                    saved = true;
                }
            } else dispose();
        });

        panel.add(applyButton);
        panel.add(ok);
        panel.add(cancel);


        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancel.doClick();
            }
        });

        return panel;
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    private void setNonSaved() {
        saved = false;
        applyButton.setEnabled(true);
    }
    private void runAfterSave(Runnable r) {
        toSaveRuns.add(r);
    }
}