package de.julianweinelt.databench.ui.editor;

import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.ui.BenchUI;

import javax.swing.*;
import java.awt.*;

public class WelcomeTab implements IEditorTab {
    @Override
    public JPanel getTabComponent(BenchUI ui, DConnection connection) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // ===== Header =====
        JLabel title = new JLabel("Workspace of " + connection.getProject().getName());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));

        JLabel subtitle = new JLabel("Get started by choosing one of the following actions.");
        subtitle.setFont(subtitle.getFont().deriveFont(14f));
        subtitle.setForeground(Color.GRAY);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(subtitle);

        root.add(header, BorderLayout.NORTH);

        // ===== Actions =====
        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setOpaque(false);

        actions.add(createActionButton("➕ New Query", connection::addEditorTab
        ));

        actions.add(Box.createVerticalStrut(10));

        actions.add(createActionButton("🧱 Create Table", connection::addCreateTableTab
        ));

        actions.add(Box.createVerticalStrut(10));

        actions.add(createActionButton("🔄 Refresh Schema", () -> {

        }));

        root.add(actions, BorderLayout.CENTER);

        return root;
    }

    @Override
    public String getTitle() {
        return "Welcome";
    }

    private JButton createActionButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 15f));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setFocusPainted(false);

        button.addActionListener(e -> action.run());
        return button;
    }
}
