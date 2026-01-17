package de.julianweinelt.databench.ui.plugins;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PluginListPanel extends JPanel {

    private final JPanel listPanel;
    private final List<PluginDescriptor> plugins = new ArrayList<>();

    public PluginListPanel(boolean installedTab, Consumer<PluginDescriptor> onSelect) {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search plugins...");
        add(searchField, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        for (int i = 1; i <= 10; i++) {
            PluginDescriptor p = new PluginDescriptor(
                    "plugin-" + i,
                    (installedTab ? "Installed " : "Plugin ") + i,
                    "1.0." + i,
                    "This is a short description of the plugin.",
                    installedTab
            );
            if (installedTab && i % 2 == 0) p.setUpdateAvailable(true);
            plugins.add(p);
        }

        rebuildList(plugins, onSelect);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String query = searchField.getText().toLowerCase();
                rebuildList(
                        plugins.stream()
                                .filter(p -> p.getName().toLowerCase().contains(query))
                                .toList(),
                        onSelect
                );
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });
    }

    private void rebuildList(List<PluginDescriptor> data, Consumer<PluginDescriptor> onSelect) {
        listPanel.removeAll();

        for (PluginDescriptor plugin : data) {
            PluginCardPanel card = new PluginCardPanel(plugin);
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onSelect.accept(plugin);
                }
            });
            listPanel.add(card);
            listPanel.add(Box.createVerticalStrut(8));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }
}
