package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.DataBench;
import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.data.ConfigManager;
import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.data.ProjectManager;
import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.events.Event;
import de.julianweinelt.databench.dbx.api.events.Subscribe;
import de.julianweinelt.databench.dbx.api.ui.ShortcutManager;
import de.julianweinelt.databench.dbx.api.ui.menubar.MenuBar;
import de.julianweinelt.databench.dbx.database.DatabaseRegistry;
import de.julianweinelt.databench.service.UpdateChecker;
import de.julianweinelt.databench.ui.plugins.PluginDialog;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static de.julianweinelt.databench.dbx.util.LanguageManager.translate;

@Slf4j
@Getter
public class BenchUI {
    private final HashMap<Project, DConnection> connections = new HashMap<>();

    private JFrame frame;
    private JTabbedPane tabbedPane;

    private MenuBar menuBar;

    private JPanel cardsContainer;

    public void loadTheme(boolean showDialogOnError) {
        log.debug("Loading theme data...");
        String selected = Configuration.getConfiguration().getSelectedTheme();
        String definingPlugin = selected.split(":")[0];
        String theme = selected.split(":")[1];
        log.info("Setting theme to \"{}\" by {}", theme, definingPlugin);
        ThemeSwitcher.switchTheme(theme, Registry.instance().getPlugin(definingPlugin), showDialogOnError);
    }

    public void init() {
        Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));

        tabbedPane = new JTabbedPane();

        frame = new JFrame();
        DataBench.getInstance().setOverFrame(frame);
        createMenuBar();
        frame.setFont(Configuration.getConfiguration().getEditorFontObject());
        frame.setIconImage(icon);
        frame.setSize(1024, 600);
        frame.setLocationRelativeTo(null);
        if (Configuration.getConfiguration().isStoppedMaximized()) frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        frame.setName("DataBench");
        frame.setLayout(new BorderLayout());

        registerShortcuts(frame);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        createStartPage();
        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setVisible(true);

        UpdateChecker.instance().checkForUpdates(false);
        frame.setTitle(translate("main.title", Map.of("version", DataBench.version)));
        if (Configuration.getConfiguration().isFirstStartup()) {
            showDataSendDialog();
        }
    }

    private void registerShortcuts(JFrame frame) {
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(Configuration.getConfiguration().getShortcut("NEW_FILE",
                        ShortcutManager.instance().getAction("NEW_FILE").defaultKey()), "create-object");

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
                            if (connections.isEmpty()) {
                                createLightEdit();
                                try {Thread.sleep(100);} catch (InterruptedException ignored) {}
                                DConnection c = connections.get(ProjectManager.LIGHT_EDIT_PROJECT);
                                switch (type) {
                                    case TABLE -> c.addCreateTableTab("db");
                                    case VIEW -> JOptionPane.showMessageDialog(frame,
                                            "View creation not yet implemented.", "Info",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    case PROCEDURE -> JOptionPane.showMessageDialog(frame,
                                            "Stored Procedure creation not yet implemented.", "Info",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    case FUNCTION -> JOptionPane.showMessageDialog(frame,
                                            "Function creation not yet implemented.", "Info",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    case SCHEMA -> c.addEditorTab("CREATE DATABASE ${name};");
                                }
                            } else {
                                int selectedIndex = tabbedPane.getSelectedIndex();
                                if (selectedIndex == -1) return;
                                DConnection c = connections.values().stream().toList().get(selectedIndex - 1);
                                if (c == null) return;

                                switch (type) {
                                    case TABLE -> c.addCreateTableTab("db");
                                    case VIEW -> JOptionPane.showMessageDialog(frame,
                                            "View creation not yet implemented.", "Info",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    case PROCEDURE -> JOptionPane.showMessageDialog(frame,
                                            "Stored Procedure creation not yet implemented.", "Info",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    case FUNCTION -> JOptionPane.showMessageDialog(frame,
                                            "Function creation not yet implemented.", "Info",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    case SCHEMA -> c.addEditorTab("CREATE DATABASE ${name};");
                                }
                            }
                        }
                    }
                });
    }


    public void connect(Project project) {
        log.info("Opening normal project {}", project.getUuid());
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        DConnection connection = new DConnection(project, this);
        connections.put(project, connection);
        connection.connect().thenAccept(conn -> {
            connection.createNewConnectionTab();
            frame.setCursor(Cursor.getDefaultCursor());
        }).exceptionally(ex -> {
            frame.setCursor(Cursor.getDefaultCursor());
            if (ex.getMessage().contains("No suitable driver found")) {
                JOptionPane.showMessageDialog(frame, translate("dialog.profile.open.driver.not-found.description"),
                        translate("dialog.profile.open.driver.not-found.title"), JOptionPane.ERROR_MESSAGE);
                return null;
            } else
                JOptionPane.showMessageDialog(frame, translate("dialog.profile.open.driver.no-connection.description"),
                        translate("dialog.profile.open.driver.no-connection.title"), JOptionPane.ERROR_MESSAGE);
            connections.remove(project);
            if (ex instanceof ClassNotFoundException) return null;
            connection.createNewConnectionTab();
            log.error(ex.getMessage(), ex);
            return null;
        });
    }

    public DConnection createLightEdit() {
        Project project = ProjectManager.LIGHT_EDIT_PROJECT;
        log.info("Opening project light edit {}", project.getUuid());
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        DConnection connection = new DConnection(project, this, true);
        connections.put(project, connection);
        connection.connect().thenAccept(conn -> {
            connection.createNewConnectionTab();
            frame.setCursor(Cursor.getDefaultCursor());
        }).exceptionally(ex -> {
            frame.setCursor(Cursor.getDefaultCursor());
            connections.remove(project);
            if (ex instanceof ClassNotFoundException) return null;
            connection.createNewConnectionTab();
            log.error(ex.getMessage(), ex);
            return null;
        });
        return connection;
    }
    public boolean hasLightEdit() {
        return connections.containsKey(ProjectManager.LIGHT_EDIT_PROJECT);
    }


    /**
     * Retrieves the connection associated with the LIGHT_EDIT_PROJECT, if available.
     *
     * @return the DConnection associated with LIGHT_EDIT_PROJECT, or null if no connection exists.
     */
    @Nullable
    public DConnection getLightEdit() {
        return connections.getOrDefault(ProjectManager.LIGHT_EDIT_PROJECT, null);
    }

    public void updateProjectCards() {
        cardsContainer = new JPanel();
        cardsContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        cardsContainer.setOpaque(false);

        for (Project p : ProjectManager.instance().getProjects()) {
            JPanel card = p.createCard(this);
            cardsContainer.add(card);
        }
        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    private void createStartPage() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);

        JLabel title = new JLabel(translate("screen.main.welcome"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton createProfile = new JButton("➕ " + translate("screen.main.profile.create"));
        createProfile.addActionListener(e -> showAddProfilePopup());

        JButton importProfile = new JButton("📂 " + translate("screen.main.profile.import"));
        importProfile.addActionListener(e -> showImportProfilePopup());

        buttonPanel.add(createProfile);
        buttonPanel.add(importProfile);

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        updateProjectCards();

        JScrollPane cardsScroll = new JScrollPane(cardsContainer);
        cardsScroll.setBorder(BorderFactory.createEmptyBorder());
        cardsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cardsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JComponent newsPanel = createNewsPanel();

        JPanel tipsPanel = new JPanel();
        tipsPanel.setLayout(new BoxLayout(tipsPanel, BoxLayout.Y_AXIS));
        tipsPanel.setBorder(BorderFactory.createTitledBorder("Next Steps & Documentation"));
        tipsPanel.setOpaque(false);

        JLabel tip1 = new JLabel("• All database connections are organized in projects.");
        JLabel tip2 = new JLabel("• You can securely share them by exporting.");
        JLabel tip3 = new JLabel("<html>• Check the <a href='https://github.com/JWeinelt/DataBench/wiki'>documentation</a> for SQL tips.</html>");

        tipsPanel.add(tip1);
        tipsPanel.add(tip2);
        tipsPanel.add(tip3);

        JSplitPane bottomSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                newsPanel,
                tipsPanel
        );
        bottomSplit.setResizeWeight(0.7);
        bottomSplit.setOneTouchExpandable(true);
        bottomSplit.setDividerSize(8);

        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                cardsScroll,
                bottomSplit
        );
        mainSplit.setResizeWeight(0.75);
        mainSplit.setDividerSize(10);
        mainSplit.setOneTouchExpandable(false);

        mainPanel.add(mainSplit, BorderLayout.CENTER);

        addNonClosableTab(tabbedPane, translate("screen.main.home"), mainPanel);
    }

    //TODO: Make news dynamic
    private JComponent createNewsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(translate("startpage.news.title")));

        JPanel header = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Example");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));

        JLabel dateLabel = new JLabel("2026-01-15");
        dateLabel.setFont(dateLabel.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(dateLabel);

        JButton toggleButton = new JButton("−");
        toggleButton.setMargin(new Insets(1, 1, 1, 1));
        toggleButton.addActionListener(e -> {
            panel.setVisible(!panel.isVisible());
        });

        header.add(titleBox, BorderLayout.WEST);
        header.add(toggleButton, BorderLayout.EAST);

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setText("""
        <html>
            <body style='font-family:Segoe UI;'>
                <p>This is <b>HTML content</b>.</p>
                <p>You can show images:</p>
                <img src="https://via.placeholder.com/300x120"/>
            </body>
        </html>
    """);

        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        JScrollPane scrollPane = new JScrollPane(editorPane);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        panel.putClientProperty("toggleButton", toggleButton);

        return panel;
    }


    private void showAddProfilePopup() {
        JDialog popup = new JDialog(frame, translate("screen.main.profile.ui.add"), true);
        addEscapeKeyBind(popup);

        popup.setResizable(false);
        popup.setSize(500, 350);
        popup.setLayout(new GridBagLayout());
        popup.setLocationRelativeTo(frame);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;


        String[] dbTypes = new String[DatabaseRegistry.instance().getDatabaseTypes().size()];
        for (int i = 0; i < dbTypes.length; i++) {
            dbTypes[i] = DatabaseRegistry.instance().getDatabaseTypes().get(i).engineName();
        }
        JLabel typeLabel = new JLabel("Database Type:");
        JComboBox<String> dbType = new JComboBox<>(dbTypes);

        JLabel nameLabel = new JLabel(translate("screen.main.profile.ui.form.name"));
        JTextField nameField = new JTextField();

        JLabel hostLabel = new JLabel(translate("screen.main.profile.ui.form.host"));
        JTextField hostField = new JTextField();

        JLabel userLabel = new JLabel(translate("screen.main.profile.ui.form.username"));
        JTextField userField = new JTextField();

        JLabel passwordLabel = new JLabel(translate("screen.main.profile.ui.form.password"));
        JPasswordField passwordField = new JPasswordField();

        JCheckBox sslCheck = new JCheckBox(translate("screen.main.profile.ui.form.useSSL"));

        JLabel dbLabel = new JLabel(translate("screen.main.profile.ui.form.defaultDB"));
        JTextField dbField = new JTextField();

        JLabel useWinAuthLabel = new JLabel(translate("screen.main.profile.ui.form.winauth"));
        JCheckBox winAuthField = new JCheckBox();
        winAuthField.setVisible(false);
        useWinAuthLabel.setVisible(false);

        JLabel resultLabel = new JLabel("");
        resultLabel.setForeground(Color.RED);


        dbType.addActionListener(e -> {
            String type = (dbType.getSelectedItem() == null) ? "MySQL" : dbType.getSelectedItem().toString();

            userField.setEditable(!type.equalsIgnoreCase("mssql"));
            passwordField.setEditable(!type.equalsIgnoreCase("mssql"));
            winAuthField.setVisible(type.equalsIgnoreCase("mssql"));
            useWinAuthLabel.setVisible(type.equalsIgnoreCase("mssql"));
        });

        JButton createButton = new JButton(translate("screen.main.profile.ui.form.button.create"));
        JButton testButton = new JButton(translate("screen.main.profile.ui.form.button.test"));


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
        gbc.gridx = 0;
        gbc.gridy = row;
        popup.add(useWinAuthLabel, gbc);
        gbc.gridx = 1;
        popup.add(winAuthField, gbc);

        row++;
        gbc.gridx = 1;
        gbc.gridy = row;
        popup.add(sslCheck, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        popup.add(typeLabel, gbc);
        gbc.gridx = 1;
        popup.add(dbType, gbc);

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

        createButton.addActionListener(e -> {
            String type = (dbType.getSelectedItem() == null) ? "MySQL" : dbType.getSelectedItem().toString();
            if (nameField.getText().isBlank()) {
                resultLabel.setText(translate("screen.main.profile.ui.feedback.noName"));
                return;
            }
            if (hostField.getText().isBlank()) {
                resultLabel.setText(translate("screen.main.profile.ui.feedback.noHost"));
                return;
            }
            if (userField.getText().isBlank() && !type.equalsIgnoreCase("mssql")) {
                resultLabel.setText(translate("screen.main.profile.ui.feedback.noUser"));
                return;
            }
            if (passwordField.getPassword().length == 0 && !type.equalsIgnoreCase("mssql")) {
                resultLabel.setText(translate("screen.main.profile.ui.feedback.noPassword"));
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
                    defaultDB,
                    type
            );

            try {
                ProjectManager.instance().addProject(project, Configuration.getConfiguration().getEncryptionPassword());
                ProjectManager.instance().saveProjectFile(project, Configuration.getConfiguration().getEncryptionPassword());

                JPanel card = project.createCard(this);
                cardsContainer.add(card);
                updateProjectCards();
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
            String type = (dbType.getSelectedItem() == null) ? "MySQL" : dbType.getSelectedItem().toString();

            Project testProject = new Project(
                    nameField.getText(),
                    hostPort,
                    username,
                    password,
                    useSSL,
                    defaultDB,
                    type
            );

            boolean success = new DConnection(testProject, this).testConnection();
            if (success) {
                resultLabel.setForeground(Color.GREEN);
                resultLabel.setText(translate("screen.main.profile.ui.feedback.test.success"));
            } else {
                resultLabel.setForeground(Color.RED);
                resultLabel.setText(translate("screen.main.profile.ui.feedback.test.fail"));
            }
        });

        popup.setVisible(true);
    }



    private void createMenuBar() {
        menuBar = new de.julianweinelt.databench.dbx.api.ui.menubar.MenuBar(frame,
                DataBench.getInstance().getApi().getSystemPlugin())
                .disable("edit", "sql").disable("file", 0).updateAll();
    }

    public void addClosableTab(JTabbedPane tabbedPane, String title, Component content) {
        tabbedPane.add(content);
        int index = tabbedPane.indexOfComponent(content);

        JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.setOpaque(false);

        JLabel label = new JLabel(title + " ");
        JButton closeButton = getCloseButton(tabbedPane, content);

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

                if (connections.size() <= i - 1) {
                    log.warn("Closing last connection. Idx mismatch!");
                    return;
                }

                Project toClose = connections.keySet().stream().toList().get(i -1);
                DConnection connection = connections.get(toClose);
                connection.disconnect();
                connections.remove(toClose);
            }
        };

        tabPanel.addMouseListener(mouseAdapter);
        label.addMouseListener(mouseAdapter);

        tabbedPane.setTabComponentAt(index, tabPanel);
    }

    private @NotNull JButton getCloseButton(JTabbedPane tabbedPane, Component content) {
        JButton closeButton = new JButton("x");
        closeButton.setMargin(new Insets(0, 2, 0, 2));
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setFocusable(false);
        closeButton.setContentAreaFilled(false);

        closeButton.addActionListener(e -> {
            int i = tabbedPane.indexOfComponent(content);
            if (i != -1) tabbedPane.remove(i);

            if (connections.size() <= i - 1) {
                log.warn("Closing last connection. Closing project. Idx mismatch!");
                return;
            }

            Project toClose = connections.keySet().stream().toList().get(i -1);
            DConnection connection = connections.get(toClose);
            connection.disconnect();
            connections.remove(toClose);
        });
        return closeButton;
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

    private void showImportProfilePopup() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(translate("dialog.profile.import.title"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                translate("dialog.save.extension.dbproj"), "dbproj"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null || !selectedFile.exists()) return;

        JPanel passwordPanel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel(translate("dialog.profile.import.password"));
        JPasswordField passField = new JPasswordField();
        passwordPanel.add(label, BorderLayout.NORTH);
        passwordPanel.add(passField, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(frame, passwordPanel,
                translate("dialog.profile.import.password.title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION) return;

        String password = new String(passField.getPassword());

        try {
            Project project = Project.loadFromFile(selectedFile, password);
            if (project == null) {
                JOptionPane.showMessageDialog(frame,
                        translate("dialog.profile.import.error.password"),
                        "dialog.profile.import.error.title", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (ProjectManager.instance().projectExists(project.getName())) {
                JOptionPane.showMessageDialog(frame,
                        translate("dialog.profile.import.error.duplicate"),
                        translate("dialog.profile.import.warning.title"), JOptionPane.WARNING_MESSAGE);
                project.setName(project.getName() + "_1");
            }
            ProjectManager.instance().addProject(project, password);
            ProjectManager.instance().saveProjectFile(project, password);

            JPanel card = project.createCard(this);
            cardsContainer.add(card);
            cardsContainer.revalidate();
            cardsContainer.repaint();

            JOptionPane.showMessageDialog(frame,
                    translate("dialog.profile.import.success", Map.of("name", project.getName())),
                    translate("dialog.profile.import.success.title"), JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    translate("dialog.profile.import.error", Map.of("error", ex.getMessage())),
                    translate("dialog.profile.import.error.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public void importProfilePopupPreDefinedFile(File file) {
        JPanel passwordPanel = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel(translate("dialog.profile.import.password"));
        JPasswordField passField = new JPasswordField();
        passwordPanel.add(label, BorderLayout.NORTH);
        passwordPanel.add(passField, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(frame, passwordPanel,
                translate("dialog.profile.import.password.title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION) return;

        String password = new String(passField.getPassword());

        try {
            Project project = Project.loadFromFile(file, password);
            if (project == null) {
                JOptionPane.showMessageDialog(frame,
                        translate("dialog.profile.import.error.password"),
                        "dialog.profile.import.error.title", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (ProjectManager.instance().projectExists(project.getName())) {
                JOptionPane.showMessageDialog(frame,
                        translate("dialog.profile.import.error.duplicate"),
                        translate("dialog.profile.import.warning.title"), JOptionPane.WARNING_MESSAGE);
                project.setName(project.getName() + "_1");
            }
            ProjectManager.instance().addProject(project, password);
            ProjectManager.instance().saveProjectFile(project, password);

            JPanel card = project.createCard(this);
            cardsContainer.add(card);
            cardsContainer.revalidate();
            cardsContainer.repaint();

            JOptionPane.showMessageDialog(frame,
                    translate("dialog.profile.import.success", Map.of("name", project.getName())),
                    translate("dialog.profile.import.success.title"), JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    translate("dialog.profile.import.error", Map.of("error", ex.getMessage())),
                    translate("dialog.profile.import.error.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showLicenseInfo() {
        JDialog dialog = new JDialog(frame, translate("dialog.license.title"), true);
        addEscapeKeyBind(dialog);
        dialog.setResizable(false);
        dialog.setSize(600, 400);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(frame);

        String licenseText = """
                Copyright (C) 2025–2026 Julian Weinelt
               \s
                This program is free software: you can redistribute it and/or modify
                it under the terms of the GNU General Public License as published by
                the Free Software Foundation, either version 3 of the License, or
                (at your option) any later version.
               \s
                This program is distributed in the hope that it will be useful,
                but WITHOUT ANY WARRANTY; without even the implied warranty of
                MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
                GNU General Public License for more details.
               \s
                You should have received a copy of the GNU General Public License
                along with this program. If not, see <https://www.gnu.org/licenses/>.
               \s
                ---
               \s
                Third-Party Libraries
               \s
                This software uses the following third-party libraries.
                Each library is distributed under its own license.
               \s
                ---
               \s
                Logback (logback-classic)
                Copyright (C) 1999–2024 QOS.ch
               \s
                License: Eclipse Public License v1.0 / GNU LGPL 2.1
               \s
                This library is dual-licensed under the Eclipse Public License 1.0
                and the GNU Lesser General Public License 2.1.
               \s
                ---
               \s
                Jackson Databind
                Copyright (C) FasterXML, LLC
               \s
                License: Apache License 2.0
               \s
                Licensed under the Apache License, Version 2.0 (the "License");
                you may not use this file except in compliance with the License.
                You may obtain a copy of the License at
               \s
                https://www.apache.org/licenses/LICENSE-2.0
               \s
                ---
               \s
                Gson
                Copyright (C) Google Inc.
               \s
                License: Apache License 2.0
               \s
                Licensed under the Apache License, Version 2.0 (the "License");
                you may not use this file except in compliance with the License.
                You may obtain a copy of the License at
               \s
                https://www.apache.org/licenses/LICENSE-2.0
               \s
                ---
               \s
                Lombok
                Copyright (C) 2009–2024 The Project Lombok Authors
               \s
                License: MIT License
               \s
                Permission is hereby granted, free of charge, to any person obtaining
                a copy of this software and associated documentation files (the
                "Software"), to deal in the Software without restriction...
               \s
                ---
               \s
                FlatLaf
                Copyright (C) FormDev Software GmbH
               \s
                License: Apache License 2.0
               \s
                Licensed under the Apache License, Version 2.0 (the "License");
                you may not use this file except in compliance with the License.
                You may obtain a copy of the License at
               \s
                https://www.apache.org/licenses/LICENSE-2.0
               \s
                ---
               \s
                JetBrains Annotations
                Copyright (C) JetBrains s.r.o.
               \s
                License: Apache License 2.0
               \s
                Licensed under the Apache License, Version 2.0 (the "License");
                you may not use this file except in compliance with the License.
                You may obtain a copy of the License at
               \s
                https://www.apache.org/licenses/LICENSE-2.0
               \s
                ---
               \s
                RSyntaxTextArea
                Copyright (C) Robert Futrell
               \s
                License: BSD 3-Clause License
               \s
                Redistribution and use in source and binary forms, with or without
                modification, are permitted provided that the following conditions
                are met...
               \s
                ---
               \s
                AutoComplete
                Copyright (C) Robert Futrell
               \s
                License: BSD 3-Clause License
               \s
                Redistribution and use in source and binary forms, with or without
                modification, are permitted provided that the following conditions
                are met...
               \s
               \s
                Apache Commons Compress
                Copyright (C) The Apache Software Foundation
               \s
                License: Apache License 2.0
               \s
                Licensed under the Apache License, Version 2.0 (the "License");
                you may not use this file except in compliance with the License.
                You may obtain a copy of the License at
               \s
                https://www.apache.org/licenses/LICENSE-2.0
       \s""";

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

    public void showChangelog() {
        JDialog dialog = new JDialog(frame, translate("dialog.changelog.title"), true);
        addEscapeKeyBind(dialog);
        dialog.setResizable(false);
        dialog.setSize(600, 400);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(frame);

        //TODO: Fetch latest changelog from server
        String licenseText = """
                
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

    public void showDataSendDialog() {
        JDialog dialog = new JDialog(frame, translate("dialog.data.title"), true);
        addEscapeKeyBind(dialog);
        dialog.setResizable(false);
        dialog.setSize(600, 400);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(frame);

        String licenseText = translate("anonymous-data.text");

        JTextArea textArea = new JTextArea(licenseText);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Arial", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton allowButton = new JButton(translate("anonymous-data.allow"));
        allowButton.addActionListener(e -> {
            Configuration.getConfiguration().setSendAnonymousData(true);
            ConfigManager.getInstance().saveConfig();
            dialog.dispose();
        });

        JButton denyButton = new JButton(translate("anonymous-data.deny"));
        denyButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(allowButton);
        buttonPanel.add(denyButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    public void stop() {
        Configuration.getConfiguration().setStoppedMaximized(frame.getExtendedState() == JFrame.MAXIMIZED_BOTH);

        if (connections.isEmpty()) {
            log.info("No Connections found. Exiting...");
        } else {
            log.info("Closing connections...");
            for (Project project : connections.keySet()) {
                DConnection connection = connections.get(project);
                connection.handleWindowClosing(frame);
            }
        }
    }

    public static void addEscapeKeyBind(RootPaneContainer container) {
        JRootPane rootPane = container.getRootPane();

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(Configuration.getConfiguration().getShortcut(
                        "ESCAPE",
                        ShortcutManager.instance().getAction("ESCAPE").defaultKey()
                ), "escape");

        rootPane.getActionMap()
                .put("escape", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (container instanceof Window w) {
                            w.dispose();
                        }
                    }
                });
    }

    @Subscribe(value = "UIMenuBarItemClickEvent")
    public void onMenuBarClickItem(Event e) {
        String id = e.get("id").asString();
        if (id.equals("file_light_edit")) createLightEdit();
        if (id.equals("file_preferences")) new SettingsDialog(frame).setVisible(true);
        if (id.equals("file_plugins")) new PluginDialog(frame).setVisible(true);
        if (id.equals("file_exit")) System.exit(0);
        if (id.equals("help_license")) showLicenseInfo();
        if (id.equals("help_show_changelog")) showChangelog();
        if (id.equals("help_data_sending")) showDataSendDialog();

        if (id.equals("edit_export")) new ExportDialog(frame).setVisible(true);
        if (id.equals("edit_import")) new ImportDialog(frame).setVisible(true);
    }
}