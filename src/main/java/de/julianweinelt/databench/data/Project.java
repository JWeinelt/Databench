package de.julianweinelt.databench.data;

import de.julianweinelt.databench.ui.BenchUI;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@Getter
@Setter
public class Project {
    private String name;
    private String server;
    private String username;
    private String password;
    private String defaultDatabase = "";
    private boolean useSSL = false;

    private final int formatVersion = 1;

    public Project(String name, String server, String username, String password, boolean useSSL, String defaultDatabase) {
        this.name = name;
        this.server = server;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
        this.defaultDatabase = defaultDatabase;
    }

    public JPanel createCard(BenchUI ui) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(180, 120));
        card.setMaximumSize(new Dimension(180, 120));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(new Color(64, 64, 64));

        JLabel titleLabel = new JLabel(name);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        card.add(titleLabel);

        card.add(Box.createVerticalGlue());

        JButton connectButton = new JButton("Connect");
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectButton.addActionListener(e -> ui.connect(this));
        card.add(connectButton);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                card.setBackground(new Color(80, 80, 80));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                card.setBackground(new Color(64, 64, 64));
            }
        });
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    ui.connect(Project.this);
                }
            }
        });

        return card;
    }
}