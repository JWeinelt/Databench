package de.julianweinelt.databench.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import de.julianweinelt.databench.api.DConnection;
import de.julianweinelt.databench.data.ConfigManager;
import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.data.Project;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

@Getter
public class BenchUI {
    private final HashMap<Project, DConnection> connections = new HashMap<>();

    private JFrame frame;
    private JPanel currentPanel;
    private JTabbedPane tabbedPane;

    public void start() {
        FlatDarkLaf.setup();
        tabbedPane = new JTabbedPane();

        frame = new JFrame();
        frame.setBounds(50, 50, 1600, 900);
        frame.setName("DataBench");
        frame.setTitle("DataBench");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());

        createMenuBar();
        createStartPage();
        frame.add(tabbedPane);

        frame.setVisible(true);
    }

    public void connect(Project project) {
        DConnection connection = new DConnection(project);
        connections.put(project, connection);
        connection.connect().thenAccept(conn -> {
            createNewConnectionTab(connection, this);
        }).exceptionally(ex -> {
            JOptionPane.showMessageDialog(frame, "No connection could be established.", "Failure", JOptionPane.ERROR_MESSAGE);
            connections.remove(project);
            createNewConnectionTab(connection, this);
            return null;
        });
    }

    private void createStartPage() {
        JPanel mainPanel = new JPanel();
        //mainPanel.setLayout(new FlowLayout());
        mainPanel.setPreferredSize(frame.getSize());

        JLabel title = new JLabel("DataBench");
        Font originalFont = title.getFont();
        Font resizedFont = originalFont.deriveFont(Font.BOLD, 24f);
        title.setFont(resizedFont);
        mainPanel.add(title);

        JPanel cards = new JPanel();
        GridLayout l = new GridLayout();
        l.setColumns(5);
        l.setRows(2);
        cards.setLayout(l);
        cards.setPreferredSize(new Dimension(frame.getSize().width - 200, frame.getSize().height - 400));

        for (Project p : Configuration.getConfiguration().getProjects()) cards.add(p.createCard(this));

        JButton createProfile = new JButton("Add profile");
        createProfile.addActionListener(e -> {
            JTextField nameField = new JTextField(10);
            JTextField connectToField = new JTextField(10);
            JTextField portField = new JTextField(5);
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();

            JFrame popup = new JFrame("Add profile");
            popup.setResizable(false);
            popup.setLocation(frame.getX() + frame.getWidth() / 2 - 300, frame.getY() + frame.getHeight() / 2 - 60);
            popup.setSize(600, 120);
            popup.setLayout(new FlowLayout());

            popup.add(new JLabel("Name: "));
            popup.add(nameField);

            popup.add(new JLabel("Connect to: "));
            popup.add(connectToField);

            popup.add(new JLabel("Port: "));
            popup.add(portField);

            popup.add(new JLabel("Username: "));
            popup.add(usernameField);

            popup.add(new JLabel("Password: "));
            popup.add(passwordField);
            JLabel resultLabel = new JLabel("");

            JButton create = new JButton("Create");
            create.addActionListener(ev -> {
                if (nameField.getText().isBlank()) {
                    resultLabel.setText("Please enter a valid name");
                    resultLabel.setForeground(Color.RED);
                    return;
                }
                if (connectToField.getText().isBlank() || portField.getText().isBlank() || !isInt(portField.getText())) {
                    resultLabel.setText("Please enter a valid address and port");
                    resultLabel.setForeground(Color.RED);
                    return;
                }
                if (usernameField.getText().isBlank()) {
                    resultLabel.setText("Please enter a valid username");
                    resultLabel.setForeground(Color.RED);
                    return;
                }
                if (passwordField.getPassword().length == 0) {
                    resultLabel.setText("Please enter a valid username");
                    resultLabel.setForeground(Color.RED);
                    return;
                }
                Configuration.getConfiguration().addProject(new Project(
                        nameField.getText(),
                        connectToField.getText() + ":" + portField.getText(),
                        usernameField.getText(),
                        new String(passwordField.getPassword())
                        ));
                ConfigManager.getInstance().saveConfig();
                popup.dispose();
            });
            JLabel escape = new JLabel("");
            escape.setSize(600, 20);
            popup.add(escape);
            popup.add(create);
            JButton testConn = new JButton("Test connection");
            testConn.addActionListener(ev -> {
                Project testProject = new Project(
                        nameField.getText(),
                        connectToField.getText() + ":" + portField.getText(),
                        usernameField.getText(),
                        new String(passwordField.getPassword()));
                if (new DConnection(testProject).testConnection()) {
                    resultLabel.setText("Test connection successful");
                    resultLabel.setForeground(Color.GREEN);
                } else {
                    resultLabel.setText("Test connection failed");
                    resultLabel.setForeground(Color.RED);
                }
            });
            popup.add(testConn);
            popup.add(resultLabel);

            popup.setVisible(true);
        });
        mainPanel.add(createProfile);

        mainPanel.add(cards);
        addClosableTab(tabbedPane, "Home", mainPanel);
    }
    private void createNewConnectionTab(DConnection connection, BenchUI ui) {
        JPanel panel = new JPanel(new BorderLayout());

        JTabbedPane workTabs = new JTabbedPane();
        connection.setWorkTabs(workTabs);
        panel.setPreferredSize(new Dimension(frame.getSize().width - 50, frame.getSize().height - 100));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(new JButton("Connect"));
        JButton btnNewQuery = new JButton("New Query");
        btnNewQuery.addActionListener(e -> {
            addEditorTab(workTabs, "New query");
        });
        toolBar.add(btnNewQuery);
        toolBar.add(new JButton("Refresh"));

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(connection.getProject().getName());
        connection.getProjectTree(root);

        JTree tree = new JTree(root);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = tree.getRowForLocation(e.getX(), e.getY());
                    if (row == -1) return;

                    tree.setSelectionRow(row);
                    DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode) tree.getPathForRow(row).getLastPathComponent();

                    JPopupMenu menu = connection.createContextMenu(node, ui);
                    if (menu != null && menu.getComponentCount() > 0) {
                        menu.show(tree, e.getX(), e.getY());
                    }
                }
            }
        });
        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setPreferredSize(new Dimension(250, 600));
        workTabs.addTab("Welcome", new JLabel("Workspace for " + connection.getProject().getName()));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, workTabs);
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0);

        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        addClosableTab(tabbedPane, connection.getProject().getName(), panel);
        tabbedPane.setSelectedIndex(1);
    }



    private void createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.add("Open");
        bar.add(fileMenu);
        frame.setJMenuBar(bar);
    }

    private boolean isInt(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void addClosableTab(JTabbedPane tabbedPane, String title, Component content) {
        tabbedPane.add(content);

        int index = tabbedPane.indexOfComponent(content);

        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setOpaque(false);

        JLabel label = new JLabel(title + " ");
        JButton closeButton = new JButton("x");
        closeButton.setMargin(new Insets(0, 2, 0, 2));
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setFocusable(false);
        closeButton.setContentAreaFilled(false);

        closeButton.addActionListener(e -> {
            int i = tabbedPane.indexOfComponent(content);
            if (i != -1) {
                tabbedPane.remove(i);
            }
        });

        tabPanel.add(label);
        tabPanel.add(closeButton);

        tabbedPane.setTabComponentAt(index, tabPanel);
    }

    public void addEditorTab(JTabbedPane workTabs, String title) {
        addEditorTab(workTabs, title, "");
    }

    public void addEditorTab(JTabbedPane workTabs, String title, String content) {
        JPanel editorPanel = new JPanel(new BorderLayout());

        JToolBar editorToolBar = new JToolBar();
        editorToolBar.setFloatable(false);
        JButton runButton = new JButton("▶ Execute (CTRL+ENTER)");
        JButton saveButton = new JButton("💾 Save (CTRL+S)");
        JButton formatButton = new JButton("✨ Format (CTRL+SHIFT+F)");
        editorToolBar.add(runButton);
        editorToolBar.add(saveButton);
        editorToolBar.add(formatButton);

        RSyntaxTextArea editorArea = new RSyntaxTextArea(18, 60);
        editorArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        editorArea.setAnimateBracketMatching(true);
        editorArea.setCodeFoldingEnabled(true);
        editorArea.setHighlightCurrentLine(true);
        editorArea.setText(content);
        editorArea.setMarkOccurrences(true);
        editorArea.setAutoIndentEnabled(true);
        editorArea.setAntiAliasingEnabled(true);

        RTextScrollPane sp = new RTextScrollPane(editorArea);
        editorPanel.add(editorToolBar);
        editorPanel.add(sp, BorderLayout.CENTER);

        workTabs.addTab(title, editorPanel);
        workTabs.setSelectedComponent(editorPanel);
    }

    public void addCreateTableEditorTab(JTabbedPane workTabs, String title) {
        JPanel editorPanel = new JPanel(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JButton saveButton = new JButton("💾 Create");
        JButton cancelButton = new JButton("❌ Cancel");
        toolBar.add(saveButton);
        toolBar.add(cancelButton);

        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.add(new JLabel("Table name:"));
        JTextField tableNameField = new JTextField(20);
        namePanel.add(tableNameField);

        String[] columnNames = {"Column Name", "Data Type", "Size", "Primary Key", "Not Null", "Auto I"};
        Object[][] data = {
                {"id", "INT", 11, true, true, true},
                {"name", "VARCHAR", 255, false, false, false}
        };

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex >= 3) return Boolean.class;
                return super.getColumnClass(columnIndex);
            }
        };
        JTable columnTable = new JTable(model);
        JScrollPane tableScroll = new JScrollPane(columnTable);

        JTextArea sqlPreview = new JTextArea(5, 60);
        sqlPreview.setFont(new Font("Consolas", Font.PLAIN, 13));
        sqlPreview.setEditable(false);
        JScrollPane sqlScroll = new JScrollPane(sqlPreview);

        Runnable updateSQL = () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE ").append(tableNameField.getText()).append(" (\n");
            for (int i = 0; i < model.getRowCount(); i++) {
                String colName = model.getValueAt(i, 0).toString();
                String type = model.getValueAt(i, 1).toString();
                String length = model.getValueAt(i, 2).toString();
                boolean pk = (Boolean) model.getValueAt(i, 3);
                boolean nn = (Boolean) model.getValueAt(i, 4);
                boolean ai = (Boolean) model.getValueAt(i, 5);

                sb.append("  ").append(colName).append(" ").append(type);
                if (!length.isEmpty()) sb.append("(").append(length).append(")");
                if (nn) sb.append(" NOT NULL");
                if (ai) sb.append(" AUTO_INCREMENT");
                sb.append(",\n");

                if (pk) {
                    sb.append("  PRIMARY KEY (").append(colName).append("),\n");
                }
            }
            int lastComma = sb.lastIndexOf(",");
            if (lastComma != -1) sb.deleteCharAt(lastComma);
            sb.append("\n);");
            sqlPreview.setText(sb.toString());
        };

        tableNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {

            }

            @Override
            public void removeUpdate(DocumentEvent e) {

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSQL.run();
            }
        });
        model.addTableModelListener(e -> updateSQL.run());

        updateSQL.run();

        editorPanel.add(toolBar, BorderLayout.NORTH);
        editorPanel.add(namePanel, BorderLayout.BEFORE_FIRST_LINE);
        editorPanel.add(tableScroll, BorderLayout.CENTER);
        editorPanel.add(sqlScroll, BorderLayout.SOUTH);

        workTabs.addTab(title, editorPanel);
        workTabs.setSelectedComponent(editorPanel);
    }

}