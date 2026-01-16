package de.julianweinelt.databench.data;

import de.julianweinelt.databench.api.ImagePanel;
import de.julianweinelt.databench.dbx.database.DatabaseRegistry;
import de.julianweinelt.databench.ui.BenchUI;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.UUID;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class Project {
    private UUID uuid;
    private String name;
    private String server;
    private String username;
    private String password;
    private String defaultDatabase = "";
    private boolean useSSL = false;
    private String databaseType;

    private final int formatVersion = 1;

    public Project(String name, String server, String username, String password, boolean useSSL, String defaultDatabase, String databaseType) {
        this.databaseType = databaseType;
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.server = server;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
        this.defaultDatabase = defaultDatabase;
    }

    public Project(UUID uuid, String name, String server, String username, String password, String defaultDatabase, boolean useSSL, String databaseType) {
        this.databaseType = databaseType;
        this.uuid = uuid;
        this.name = name;
        this.server = server;
        this.username = username;
        this.password = password;
        this.defaultDatabase = defaultDatabase;
        this.useSSL = useSSL;
    }

    public JPanel createCard(BenchUI ui) {
        String theme = Configuration.getConfiguration().getSelectedTheme();
        URL iU = getClass().getResource("/icons/engine/" + DatabaseRegistry.instance().getMeta(databaseType).engineName() + ".png");
        Image img;
        JPanel card;
        if (iU != null) {
            img = new ImageIcon(iU).getImage();
            card = new ImagePanel(img);
        } else card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(180, 120));
        card.setMaximumSize(new Dimension(180, 120));
        Color bColor = (theme.contains("Light") ? new Color(246, 246, 246) : new Color(125, 125, 125));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bColor),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        log.debug("Project card for {} with theme {}", getName(), theme);
        if (theme.contains("Light"))
            card.setBackground(new Color(238, 238, 238));
        else
            card.setBackground(new Color(64, 64, 64));
        card.setLayout(new BorderLayout());


        JLabel titleLabel = new JLabel(name);
        if (theme.contains("Light")) titleLabel.setForeground(Color.BLACK);
        else titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.WEST);

        JButton menuButton = new JButton("⋮");
        menuButton.setMargin(new Insets(0, 5, 0, 5));
        menuButton.setBorder(BorderFactory.createEmptyBorder());
        menuButton.setContentAreaFilled(false);
        menuButton.setFocusPainted(false);

        JPopupMenu popupMenu = getProjectContextMenu(ui);

        menuButton.addActionListener(e -> popupMenu.show(menuButton, 0, menuButton.getHeight()));

        topPanel.add(menuButton, BorderLayout.EAST);
        card.add(topPanel, BorderLayout.NORTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                if (theme.contains("Light"))
                    card.setBackground(new Color(244, 244, 244));
                else
                    card.setBackground(new Color(80, 80, 80));
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                if (theme.contains("Light"))
                    card.setBackground(new Color(238, 238, 238));
                else
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

    private @NotNull JPopupMenu getProjectContextMenu(BenchUI ui) {
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
        copyConnectionString.addActionListener(e ->
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(username + "@" + server), null));
        JMenuItem copyJDBC = new JMenuItem("Copy JDBC URL");
        copyJDBC.addActionListener(e ->
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(
                        "jdbc:mysql://" + server + "/?user=" + username), null));
        JMenuItem open = new JMenuItem("Open Project");
        open.addActionListener(e -> ui.connect(this));
        popupMenu.add(open);
        popupMenu.add(editItem);
        popupMenu.add(exportItem);
        popupMenu.add(deleteItem);
        popupMenu.addSeparator();
        popupMenu.add(copyJDBC);
        popupMenu.add(copyConnectionString);
        return popupMenu;
    }

    public static Project loadFromFile(File file, String password) {
        try {
            return ProjectEncryptionUtil.decryptProject(file, password, Project.class);
        } catch (Exception e) {
            return null;
        }
    }

}