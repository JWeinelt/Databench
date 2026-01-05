package de.julianweinelt.databench.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.data.ConfigManager;
import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.data.Project;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

@Slf4j
@Getter
public class BenchUI {
    private final HashMap<Project, DConnection> connections = new HashMap<>();

    private JFrame frame;
    private JPanel currentPanel;
    private JTabbedPane tabbedPane;

    private MenuBar menuBar;

    public void start() {
        Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
        FlatDarkLaf.setup();
        tabbedPane = new JTabbedPane();

        frame = new JFrame();
        frame.setIconImage(icon);
        frame.setBounds(50, 50, 1600, 900);
        frame.setName("DataBench");
        frame.setTitle("DataBench");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        createMenuBar();
        createStartPage();
        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    public void connect(Project project) {
        frame.setCursor(Cursor.WAIT_CURSOR);
        DConnection connection = new DConnection(project);
        connections.put(project, connection);
        connection.connect().thenAccept(conn -> {
            connection.createNewConnectionTab(this);
            frame.setCursor(Cursor.getDefaultCursor());
        }).exceptionally(ex -> {
            JOptionPane.showMessageDialog(frame, "No connection could be established.", "Failure", JOptionPane.ERROR_MESSAGE);
            connections.remove(project);
            connection.createNewConnectionTab(this);
            frame.setCursor(Cursor.getDefaultCursor());
            return null;
        });
    }

    private void createStartPage() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ===== Top: Titel + Add Profile =====
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);

        JLabel title = new JLabel("DataBench");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));

        JButton createProfile = new JButton("➕ Add Profile");
        createProfile.addActionListener(e -> showAddProfilePopup());

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(createProfile, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel cardsContainer = new JPanel();
        cardsContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10)); // Karten nebeneinander, Abstand 10px
        cardsContainer.setOpaque(false);

        for (Project p : Configuration.getConfiguration().getProjects()) {
            JPanel card = p.createCard(this);
            cardsContainer.add(card);
        }

        JScrollPane cardsScroll = new JScrollPane(cardsContainer);
        cardsScroll.setBorder(BorderFactory.createEmptyBorder());
        cardsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cardsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        mainPanel.add(cardsScroll, BorderLayout.CENTER);

        JPanel tipsPanel = new JPanel();
        tipsPanel.setLayout(new BoxLayout(tipsPanel, BoxLayout.Y_AXIS));
        tipsPanel.setBorder(BorderFactory.createTitledBorder("Next Steps & Documentation"));
        tipsPanel.setOpaque(false);

        JLabel tip1 = new JLabel("• Create a new query by clicking on a project.");
        JLabel tip2 = new JLabel("• Explore your tables and schemas.");
        JLabel tip3 = new JLabel("<html>• Check <a href='https://github.com/JWeinelt/DataBench/wiki'>documentation</a> for SQL tips.</html>");

        tipsPanel.add(tip1);
        tipsPanel.add(tip2);
        tipsPanel.add(tip3);

        mainPanel.add(tipsPanel, BorderLayout.SOUTH);

        addNonClosableTab(tabbedPane, "Home", mainPanel);
    }

    private void showAddProfilePopup() {
        // Popup-Fenster
        JFrame popup = new JFrame("Add Profile");
        popup.setResizable(false);
        popup.setSize(500, 300);
        popup.setLayout(new GridBagLayout());
        popup.setLocationRelativeTo(frame);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ===== Eingabefelder =====
        JLabel nameLabel = new JLabel("Profile Name:");
        JTextField nameField = new JTextField();

        JLabel hostLabel = new JLabel("Server (host:port):");
        JTextField hostField = new JTextField();

        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField();

        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField();

        JCheckBox sslCheck = new JCheckBox("Use SSL");

        JLabel dbLabel = new JLabel("Default Database:");
        JTextField dbField = new JTextField();

        JLabel resultLabel = new JLabel("");
        resultLabel.setForeground(Color.RED);

        // ===== Buttons =====
        JButton createButton = new JButton("Create");
        JButton testButton = new JButton("Test Connection");

        // ===== Layout hinzufügen =====
        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        popup.add(nameLabel, gbc);
        gbc.gridx = 1;
        popup.add(nameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        popup.add(hostLabel, gbc);
        gbc.gridx = 1;
        popup.add(hostField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        popup.add(userLabel, gbc);
        gbc.gridx = 1;
        popup.add(userField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        popup.add(passwordLabel, gbc);
        gbc.gridx = 1;
        popup.add(passwordField, gbc);

        row++;
        gbc.gridx = 1;
        gbc.gridy = row;
        popup.add(sslCheck, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        popup.add(dbLabel, gbc);
        gbc.gridx = 1;
        popup.add(dbField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        popup.add(resultLabel, gbc);

        row++;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        popup.add(createButton, gbc);
        gbc.gridx = 1;
        popup.add(testButton, gbc);

        // ===== Button Actions =====
        createButton.addActionListener(e -> {
            if (nameField.getText().isBlank()) {
                resultLabel.setText("Please enter a valid profile name.");
                return;
            }
            if (hostField.getText().isBlank()) {
                resultLabel.setText("Please enter server host and port.");
                return;
            }
            if (userField.getText().isBlank()) {
                resultLabel.setText("Please enter a username.");
                return;
            }
            if (passwordField.getPassword().length == 0) {
                resultLabel.setText("Please enter a password.");
                return;
            }

            String hostPort = hostField.getText();
            String username = userField.getText();
            String password = new String(passwordField.getPassword());
            boolean useSSL = sslCheck.isSelected();
            String defaultDB = dbField.getText().isBlank() ? null : dbField.getText();

            Project project = new Project(
                    nameField.getText(),
                    hostPort,
                    username,
                    password,
                    useSSL,
                    defaultDB
            );

            Configuration.getConfiguration().addProject(project);
            ConfigManager.getInstance().saveConfig();
            popup.dispose();
        });

        testButton.addActionListener(e -> {
            String hostPort = hostField.getText();
            String username = userField.getText();
            String password = new String(passwordField.getPassword());
            boolean useSSL = sslCheck.isSelected();
            String defaultDB = dbField.getText().isBlank() ? null : dbField.getText();

            Project testProject = new Project(
                    nameField.getText(),
                    hostPort,
                    username,
                    password,
                    useSSL,
                    defaultDB
            );

            boolean success = new DConnection(testProject).testConnection();
            if (success) {
                resultLabel.setForeground(Color.GREEN);
                resultLabel.setText("Test connection successful!");
            } else {
                resultLabel.setForeground(Color.RED);
                resultLabel.setText("Test connection failed.");
            }
        });

        popup.setVisible(true);
    }



    private void createMenuBar() {
        menuBar = new MenuBar(frame, this);
        menuBar.updateAll();
        menuBar.disable("file")
                .disable("edit")
                .disable("sql");
    }

    private boolean isInt(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void addClosableTab(JTabbedPane tabbedPane, String title, Component content) {
        tabbedPane.add(content);
        int index = tabbedPane.indexOfComponent(content);

        JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.setOpaque(false);

        JLabel label = new JLabel(title + " ");
        JButton closeButton = new JButton("x");
        closeButton.setMargin(new Insets(0, 2, 0, 2));
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setFocusable(false);
        closeButton.setContentAreaFilled(false);

        closeButton.addActionListener(e -> {
            int i = tabbedPane.indexOfComponent(content);
            if (i != -1) tabbedPane.remove(i);
        });

        tabPanel.add(label, BorderLayout.CENTER);
        tabPanel.add(closeButton, BorderLayout.EAST);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = tabbedPane.indexOfTabComponent(tabPanel);
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    if (i != -1) tabbedPane.remove(i);
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    if (i != -1) tabbedPane.setSelectedIndex(i);
                }
            }
        };

        tabPanel.addMouseListener(mouseAdapter);
        label.addMouseListener(mouseAdapter);

        tabbedPane.setTabComponentAt(index, tabPanel);
    }


    private void addNonClosableTab(JTabbedPane tabbedPane, String title, Component content) {
        tabbedPane.add(content);

        int index = tabbedPane.indexOfComponent(content);

        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setOpaque(false);

        JLabel label = new JLabel(title + " ");

        tabPanel.add(label);

        tabbedPane.setTabComponentAt(index, tabPanel);
    }

    private JPanel createWelcomePanel(DConnection connection, BenchUI ui) {
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

        actions.add(createActionButton("➕ New Query", connection::addEditorTab));

        actions.add(Box.createVerticalStrut(10));

        actions.add(createActionButton("🧱 Create Table", connection::addCreateTableTab));

        actions.add(Box.createVerticalStrut(10));

        actions.add(createActionButton("🔄 Refresh Schema", () -> {

        }));

        root.add(actions, BorderLayout.CENTER);

        return root;
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