package de.julianweinelt.databench.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.api.DriverShim;
import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.data.ProjectManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLNonTransientConnectionException;
import java.util.HashMap;

@Slf4j
@Getter
public class BenchUI {
    private final HashMap<Project, DConnection> connections = new HashMap<>();

    private JFrame frame;
    private JPanel currentPanel;
    private JTabbedPane tabbedPane;

    private MenuBar menuBar;

    private JPanel cardsContainer;

    public void start() {
        Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
        switch (Configuration.getConfiguration().getSelectedTheme().toLowerCase()) {
            case "light" -> FlatLightLaf.setup();
            case "intellij" -> FlatIntelliJLaf.setup();
            case "darcula" -> FlatDarculaLaf.setup();
            case "darkmac" -> FlatMacDarkLaf.setup();
            case "lightmac" -> FlatMacLightLaf.setup();
            default -> FlatDarkLaf.setup();
        }

        tabbedPane = new JTabbedPane();

        frame = new JFrame();
        frame.setIconImage(icon);
        frame.setBounds(50, 50, 1600, 900);
        frame.setName("DataBench");
        frame.setTitle("DataBench v" + Configuration.getConfiguration().getClientVersion());
        frame.setLayout(new BorderLayout());

        registerShortcuts(frame);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connections.isEmpty()) {
                    frame.dispose();
                    return;
                }
                for (Project project : connections.keySet()) {
                    DConnection connection = connections.get(project);
                    connection.handleWindowClosing(frame);
                }
            }
        });

        createMenuBar();
        createStartPage();
        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private void registerShortcuts(JFrame frame) {
        KeyStroke newShortcut = KeyStroke.getKeyStroke(
                KeyEvent.VK_N,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() // Ctrl (Win/Linux), Cmd (macOS)
        );

        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(newShortcut, "create-object");

        frame.getRootPane().getActionMap()
                .put("create-object", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (tabbedPane.getSelectedIndex() == 0) {
                            showAddProfilePopup();
                            return;
                        }

                        CreateObjectDialog dialog = new CreateObjectDialog(frame);
                        dialog.setVisible(true);

                        var type = dialog.getSelectedType();
                        if (type != null) {
                            //handleCreate(type);
                        }
                    }
                });
    }


    public void connect(Project project) {
        log.info("Opening project " + project.getUuid());
        frame.setCursor(Cursor.WAIT_CURSOR);
        DConnection connection = new DConnection(project, this);
        connections.put(project, connection);
        connection.connect().thenAccept(conn -> {
            connection.createNewConnectionTab();
            frame.setCursor(Cursor.getDefaultCursor());
        }).exceptionally(ex -> {
            frame.setCursor(Cursor.getDefaultCursor());
            if (ex.getCause() instanceof UnknownHostException || ex instanceof SQLNonTransientConnectionException)
                JOptionPane.showMessageDialog(frame, "Could not contact database server.\nUnknown Host", "Failure", JOptionPane.ERROR_MESSAGE);
            else
                JOptionPane.showMessageDialog(frame, "No connection could be established.", "Failure", JOptionPane.ERROR_MESSAGE);
            connections.remove(project);
            if (ex instanceof ClassNotFoundException) return null;
            connection.createNewConnectionTab();
            log.error(ex.getMessage(), ex);
            return null;
        });
    }

    public void updateProjectCards() {
        cardsContainer = new JPanel();
        cardsContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        cardsContainer.setOpaque(false);

        for (Project p : ProjectManager.instance().getProjects()) {
            JPanel card = p.createCard(this);
            cardsContainer.add(card);
        }
    }

    private void createStartPage() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);

        JLabel title = new JLabel("DataBench");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton createProfile = new JButton("➕ Add Profile");
        createProfile.addActionListener(e -> showAddProfilePopup());

        JButton importProfile = new JButton("📂 Import Profile");
        importProfile.addActionListener(e -> showImportProfilePopup());

        buttonPanel.add(createProfile);
        buttonPanel.add(importProfile);

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        cardsContainer = new JPanel();
        cardsContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        cardsContainer.setOpaque(false);

        for (Project p : ProjectManager.instance().getProjects()) {
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
        JDialog popup = new JDialog(frame, "Add Profile", true);
        popup.setResizable(false);
        popup.setSize(500, 300);
        popup.setLayout(new GridBagLayout());
        popup.setLocationRelativeTo(frame);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;


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


        JButton createButton = new JButton("Create");
        JButton testButton = new JButton("Test Connection");


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

            try {
                ProjectManager.instance().addProject(project, Configuration.getConfiguration().getEncryptionPassword());
                ProjectManager.instance().saveProjectFile(project, Configuration.getConfiguration().getEncryptionPassword());

                JPanel card = project.createCard(this);
                cardsContainer.add(card);
                cardsContainer.revalidate();
                cardsContainer.repaint();
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
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

            boolean success = new DConnection(testProject, this).testConnection();
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

    private void showImportProfilePopup() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Project");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "DataBench Project Files (*.dbproj)", "dbproj"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null || !selectedFile.exists()) return;

        JPanel passwordPanel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel("Enter encryption password (if any):");
        JPasswordField passField = new JPasswordField();
        passwordPanel.add(label, BorderLayout.NORTH);
        passwordPanel.add(passField, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(frame, passwordPanel,
                "Project Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION) return;

        String password = new String(passField.getPassword());

        try {
            Project project = Project.loadFromFile(selectedFile, password);
            if (project == null) {
                JOptionPane.showMessageDialog(frame,
                        "Failed to import project. Make sure you entered the correct password.",
                        "Import Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (ProjectManager.instance().projectExists(project.getName())) {
                JOptionPane.showMessageDialog(frame,
                        "A project with the same name already exists. Please choose a different name.",
                        "Project Import", JOptionPane.WARNING_MESSAGE);
                project.setName(project.getName() + "_1");
            }
            ProjectManager.instance().addProject(project, password);
            ProjectManager.instance().saveProjectFile(project, password);

            JPanel card = project.createCard(this);
            cardsContainer.add(card);
            cardsContainer.revalidate();
            cardsContainer.repaint();

            JOptionPane.showMessageDialog(frame,
                    "Project imported successfully: " + project.getName(),
                    "Import Successful", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    "Failed to import project: " + ex.getMessage(),
                    "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showLicenseInfo() {
        JDialog dialog = new JDialog(frame, "License Information", true);
        dialog.setResizable(false);
        dialog.setSize(600, 400);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(frame);

        String licenseText = """
        This software is licensed under the GPLv3.

        Libraries used:

        rsyntaxtextarea 3.3.3 - BSD-3-Clause
        https://bobbylight.github.io/RSTALanguageSupport/

        autocomplete 3.3.2 - BSD-3-Clause
        https://bobbylight.github.io/RSTALanguageSupport/

        Logback Classic 1.5.19 - EPL-1.0
        https://logback.qos.ch/

        Jackson Databind 2.20.1 - Apache License 2.0
        https://github.com/FasterXML/jackson-databind

        Gson 2.13.1 - Apache License 2.0
        https://github.com/google/gson

        Lombok 1.18.38 - MIT
        https://projectlombok.org/

        FlatLaf 3.6 - Apache License 2.0
        https://www.formdev.com/flatlaf/

        MySQL Connector/J 9.4.0 - GPLv2 with FOSS License Exception
        https://dev.mysql.com/downloads/connector/j/
        """;

        JTextArea textArea = new JTextArea(licenseText);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }


}