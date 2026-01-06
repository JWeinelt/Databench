package de.julianweinelt.databench.data;

import de.julianweinelt.databench.api.ImagePanel;
import de.julianweinelt.databench.ui.BenchUI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Project {
    private UUID uuid = UUID.randomUUID();
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
        Image img = new ImageIcon(getClass().getResource("/icons/engine/mysql.png")).getImage();
        JPanel card = new ImagePanel(img);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(180, 120));
        card.setMaximumSize(new Dimension(180, 120));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(125, 125, 125)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(new Color(64, 64, 64));
        card.setLayout(new BorderLayout());


        JLabel titleLabel = new JLabel(name);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.WEST);

        JButton menuButton = new JButton("⋮");
        menuButton.setMargin(new Insets(0, 5, 0, 5));
        menuButton.setBorder(BorderFactory.createEmptyBorder());
        menuButton.setContentAreaFilled(false);
        menuButton.setFocusPainted(false);

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit");
        editItem.addActionListener(e -> ProjectManager.instance().showEditProjectPopup(this, ui));
        JMenuItem exportItem = new JMenuItem("Export");
        exportItem.addActionListener(e -> ProjectManager.instance().showExportPopup(ui, this));
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> {
            int val = JOptionPane.showConfirmDialog(ui.getFrame(), "Are you sure you want to delete this project?\nThis cannot be undone!");
            if (val == JOptionPane.YES_OPTION) {
                ProjectManager.instance().deleteProjectFile(this, ui);
                ui.updateProjectCards();
            }
        });

        JMenuItem copyConnectionString = new JMenuItem("Copy Connection String");
        copyConnectionString.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(username + "@" + server), null);
        });
        JMenuItem copyJDBC = new JMenuItem("Copy JDBC URL");
        copyJDBC.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("jdbc:mysql://" + server + "/?user=" + username), null);
        });
        JMenuItem open = new JMenuItem("Open Project");
        open.addActionListener(e -> {
            ui.connect(this);
        });
        popupMenu.add(open);
        popupMenu.add(editItem);
        popupMenu.add(exportItem);
        popupMenu.add(deleteItem);
        popupMenu.addSeparator();
        popupMenu.add(copyJDBC);
        popupMenu.add(copyConnectionString);

        menuButton.addActionListener(e -> popupMenu.show(menuButton, 0, menuButton.getHeight()));

        topPanel.add(menuButton, BorderLayout.EAST);
        card.add(topPanel, BorderLayout.NORTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                card.setBackground(new Color(80, 80, 80));
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                card.setBackground(new Color(64, 64, 64));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.show(card, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ui.connect(Project.this);
                }
            }
        });

        return card;
    }

    public static Project loadFromFile(File file, String password) {
        try {
            return ProjectEncryptionUtil.decryptProject(file, password, Project.class);
        } catch (Exception e) {
            return null;
        }
    }

}