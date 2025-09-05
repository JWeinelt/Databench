package de.julianweinelt.databench.data;

import de.julianweinelt.databench.ui.BenchUI;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
@Setter
public class Project {
    private String name;
    private String server;
    private String username;
    private String password;
    private String defaultDatabase = "";

    public Project(String name, String server, String username, String password) {
        this.name = name;
        this.server = server;
        this.username = username;
        this.password = password;
    }

    public JPanel createCard(BenchUI ui) {
        JPanel card = new JPanel();
        card.setMaximumSize(new Dimension((ui.getFrame().getSize().width - 200) / 5, (ui.getFrame().getSize().height - 400) / 2));
        card.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        card.setBackground(new Color(94, 94, 94));
        card.add(new JLabel(name));
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> ui.connect(this));
        card.add(connectButton);
        return card;
    }
}