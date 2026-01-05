package de.julianweinelt.databench.api;

import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.ui.BenchUI;
import de.julianweinelt.databench.ui.editor.CreateTableTab;
import de.julianweinelt.databench.ui.editor.EditorTab;
import de.julianweinelt.databench.ui.editor.IEditorTab;
import de.julianweinelt.databench.ui.editor.WelcomeTab;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class DConnection {

    private final Pattern PLACEHOLDER_PATTERN =
        Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");
    private final Project project;
    private BenchUI benchUI;
    private java.sql.Connection conn;
    @Setter
    private JTabbedPane workTabs;

    @Setter
    private DefaultMutableTreeNode treeRoot;

    public DConnection(Project project) {
        this.project = project;
    }

    public void createNewConnectionTab(BenchUI ui) {
        this.benchUI = ui;
        JPanel panel = new JPanel(new BorderLayout());

        workTabs = new JTabbedPane();

        workTabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    int tabIndex = workTabs.indexAtLocation(e.getX(), e.getY());
                    if (tabIndex >= 1) {
                        workTabs.removeTabAt(tabIndex);
                    }
                }
            }
        });

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(new JButton("Connect"));
        JButton btnNewQuery = new JButton("New Query");
        btnNewQuery.addActionListener(e -> {
            addEditorTab("/* Your query goes here */");
        });
        toolBar.add(btnNewQuery);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(getProject().getName());
        setTreeRoot(root);
        getProjectTree();

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            log.info("Refreshing project tree for {}", getProject().getName());
            getProjectTree();
        });
        toolBar.add(refreshBtn);

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

                    JPopupMenu menu = createContextMenu(node, ui);
                    if (menu != null && menu.getComponentCount() > 0) {
                        menu.show(tree, e.getX(), e.getY());
                    }
                }
            }
        });
        JScrollPane treeScroll = new JScrollPane(tree);

        JSplitPane splitPane =
                new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, workTabs);
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0);

        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        ui.addClosableTab(ui.getTabbedPane(), getProject().getName(), panel);
        ui.getTabbedPane().setSelectedIndex(1);
        ui.getMenuBar().enable("file")
                .enable("edit")
                .enable("sql").updateAll();

        addTab(new WelcomeTab());
    }

    public void addCreateTableTab() {
        addTab(new CreateTableTab());
    }

    public void addEditorTab() {
        addEditorTab("");
    }

    public EditorTab addEditorTab(String content) {
        Map<String, String> values = resolvePlaceholders(content);
        if (values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                content = content.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }


        EditorTab tab = new EditorTab(content, benchUI);
        addTab(tab);
        return tab;
    }

    private Map<String, String> resolvePlaceholders(String content) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);

        Set<String> placeholders = new LinkedHashSet<>();
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }

        if (placeholders.isEmpty()) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        for (String placeholder : placeholders) {
            String value = askUserForPlaceholder(placeholder);
            if (value == null) {
                return null;
            }
            result.put(placeholder, value);
        }

        return result;
    }

    private String askUserForPlaceholder(String name) {
        return JOptionPane.showInputDialog(
                null,
                "Enter value of Placeholder \"" + name + "\":",
                "Set placeholder",
                JOptionPane.QUESTION_MESSAGE
        );
    }

    public void removeTab(IEditorTab tab) {
        for (int i = 0; i < workTabs.getTabCount(); i++) {
            Component c = workTabs.getComponentAt(i);
            if (c.equals(tab.getTabComponent(benchUI, this))) {
                workTabs.remove(i);
                break;
            }
        }
    }

    public void addTab(IEditorTab tab) {
        JPanel p = tab.getTabComponent(benchUI, this);
        workTabs.addTab(tab.getTitle(), p);
        workTabs.setSelectedIndex(workTabs.getTabCount() - 1);
    }


    public CompletableFuture<Connection> connect() {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        final String DB_NAME = "jdbc:mysql://"+project.getServer() +"/"+project.getDefaultDatabase()+
                "?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true"
                + (project.isUseSSL() ? "&useSSL=true&requireSSL=true" : "&useSSL=false");

        try {
            // Load MySQL JDBC driver class
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish a connection using the provided connection details
            conn = DriverManager.getConnection(DB_NAME, project.getUsername(), project.getPassword());
            future.complete(conn);
        } catch (Exception ex) {
            // Log any exception that occurs during the connection process
            log.warn("MySQL connection failed: {}", ex.getMessage());
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    public boolean testConnection() {
        try {
            return !connect().get().isClosed();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean checkConnection() {
        try {
            return conn != null && !conn.isClosed();
        } catch (SQLException ignored) {
            return false;
        }
    }

    public List<String> getDatabases() {
        List<String> databases = new ArrayList<>();
        try (PreparedStatement pS = conn.prepareStatement("SHOW DATABASES;")) {
            ResultSet rs = pS.executeQuery();
            while (rs.next()) databases.add(rs.getString(1));
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return databases;
    }

    public List<String> getTables(String database) {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement pS = conn.prepareStatement("USE " + database)) {pS.execute();} catch (SQLException e) {}
        try (PreparedStatement pS = conn.prepareStatement("SHOW TABLES;")) {
            ResultSet rs = pS.executeQuery();
            while (rs.next()) tables.add(rs.getString(1));
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return tables;
    }

    public SQLAnswer executeSQL(String sql) {
        SQLAnswer answer = null;
        try (PreparedStatement pS = conn.prepareStatement(sql)) {
            pS.execute();
            answer = new SQLAnswer(true, pS.getResultSet(), pS.getUpdateCount(), "Query executed successfully.");
        } catch (SQLException e) {
            log.error(e.getMessage());
            answer = new SQLAnswer(false, null, -1, e.getMessage());
        }
        return answer;
    }

    public List<String> getViews(String database) {
        List<String> views = new ArrayList<>();
        try (PreparedStatement pS = conn.prepareStatement("SHOW FULL TABLES IN " + database + " WHERE TABLE_TYPE LIKE 'VIEW';")) {
            ResultSet rs = pS.executeQuery();
            while (rs.next()) views.add(rs.getString(1));
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return views;
    }

    public void getProjectTree() {
        DefaultMutableTreeNode databases = new DefaultMutableTreeNode("Databases");
        treeRoot.add(databases);
        if (!checkConnection()) return;
        for (String s : getDatabases()) {
            DefaultMutableTreeNode db = new DefaultMutableTreeNode(s);
            databases.add(db);

            DefaultMutableTreeNode tb = new DefaultMutableTreeNode("Tables");
            DefaultMutableTreeNode views = new DefaultMutableTreeNode("Views");
            DefaultMutableTreeNode procedures = new DefaultMutableTreeNode("Procedures");
            db.add(tb);
            for (String t : getTables(s)) {
                DefaultMutableTreeNode table = new DefaultMutableTreeNode(t);
                tb.add(table);
            }
            for (String v : getViews(s)) {
                DefaultMutableTreeNode view = new DefaultMutableTreeNode(v);
                views.add(view);
            }
            db.add(views);
            db.add(procedures);
        }
    }

    public JPopupMenu createContextMenu(DefaultMutableTreeNode node, BenchUI ui) {
        Object userObject = node.getUserObject();
        String name = userObject.toString();

        JPopupMenu menu = new JPopupMenu();

        // Root
        if (name.equals("Databases")) {
            JMenuItem refresh = new JMenuItem("Refresh");
            refresh.addActionListener(e -> {

            });
            JMenuItem newDB = new JMenuItem("New Database");
            newDB.addActionListener(e -> {
                addEditorTab("CREATE DATABASE ${name};");
            });
            menu.add(refresh);
            menu.add(newDB);
        }

        else if (node.getParent() != null &&
                ((DefaultMutableTreeNode) node.getParent()).getUserObject().equals("Databases")) {
            JMenuItem drop = new JMenuItem("Drop Database");
            drop.addActionListener(e -> {
                String dbName = node.getUserObject().toString();
                EditorTab t = addEditorTab("DROP DATABASE `" + dbName + "`;");
                t.execute();
            });
            menu.add(drop);
            JMenuItem createTable = new JMenuItem("Create new Table");
            menu.add(createTable);
            JMenuItem createView = new JMenuItem("Create new View");
            menu.add(createView);
            JMenuItem createProcedure = new JMenuItem("Create new Procedure");
            menu.add(createProcedure);
        }

        else if (name.equals("Tables")) {
            JMenuItem create = new JMenuItem("Create new Table");
            create.addActionListener(e -> addCreateTableTab());
            menu.add(create);
            JMenuItem refresh = new JMenuItem("Refresh");
            refresh.addActionListener(e -> getProjectTree());
            menu.add(refresh);
        }

        else if (node.getParent() != null &&
                ((DefaultMutableTreeNode) node.getParent()).getUserObject().equals("Tables")) {
            JMenuItem edit = new JMenuItem("Edit Table");
            JMenuItem select = new JMenuItem("Select data");
            select.addActionListener(e -> {
                String tableName = node.getUserObject().toString();
                String db = ((DefaultMutableTreeNode) node.getParent().getParent()).getUserObject().toString();
                EditorTab t = addEditorTab("""
                        /** SQL-Script for querying all data from table '%s' in database '%s' **/
                        
                        SELECT * FROM `%s`.`%s`;
                        """.formatted(tableName, db, db, tableName));
                t.execute();
            });
            JMenuItem drop = new JMenuItem("Drop Table ");
            JMenuItem rename = new JMenuItem("Rename Table ");
            JMenu generatorMenu = new JMenu("SQL Generator");
            JMenuItem genCreate = new JMenuItem("Generate CREATE Statement");
            genCreate.addActionListener(e -> {
                String db = ((DefaultMutableTreeNode) node.getParent().getParent()).getUserObject().toString();
                String create = getCreateStatement(db, node.getUserObject().toString());
                addEditorTab("""
                        /** Script generated by DataBench SQL Generator **/
                        %s
                        """.formatted(create));
            });
            generatorMenu.add(genCreate);
            menu.add(edit);
            menu.add(select);
            menu.add(drop);
            menu.add(rename);
            menu.add(generatorMenu);
        }

        else if (name.equals("Views")) {
            JMenuItem create = new JMenuItem("Create new View");
            menu.add(create);
        }

        // Konkrete View
        else if (node.getParent() != null &&
                ((DefaultMutableTreeNode) node.getParent()).getUserObject().equals("Views")) {
            JMenuItem browse = new JMenuItem("Edit View ");
            JMenuItem select = new JMenuItem("Select data");
            JMenuItem drop = new JMenuItem("Drop View ");
            menu.add(browse);
            menu.add(select);
            menu.add(drop);
        }

        // Procedures-Ordner
        else if (name.equals("Procedures")) {
            JMenuItem create = new JMenuItem("Create new Procedure");
            menu.add(create);
        }

        return menu;
    }

    private String getCreateStatement(String db, String tableName) {
        String createStatement = "";
        try (PreparedStatement pS = conn.prepareStatement("SHOW CREATE TABLE " + db + "." + tableName + ";")) {
            ResultSet rs = pS.executeQuery();
            if (rs.next()) {
                createStatement = rs.getString(2);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return createStatement;
    }


    public Object[][] executeQuery(String sql) throws SQLException {
        checkConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<Object[]> rows = new ArrayList<>();

            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                rows.add(row);
            }

            return rows.toArray(new Object[0][]);
        }
    }

    public boolean isQuery(String sql) {
        if (sql == null) return false;

        String s = sql.trim().toUpperCase();
        if (s.isEmpty()) return false;

        return s.startsWith("SELECT")
                || s.startsWith("SHOW")
                || s.startsWith("DESCRIBE")
                || s.startsWith("DESC")
                || s.startsWith("EXPLAIN")
                || s.startsWith("WITH");
    }

    public String[] getColumnNames(String sql) throws SQLException {
        checkConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            String[] columns = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columns[i] = meta.getColumnLabel(i + 1); // getColumnName oder getColumnLabel
            }
            return columns;
        }
    }

    public record SQLAnswer(boolean success, ResultSet resultSet, int updateCount, String message) {

    }
}
