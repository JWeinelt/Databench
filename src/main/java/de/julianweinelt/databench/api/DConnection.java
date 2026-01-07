package de.julianweinelt.databench.api;

import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.ui.BenchUI;
import de.julianweinelt.databench.ui.editor.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.julianweinelt.databench.ui.LanguageManager.translate;

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
    private final List<IEditorTab> editorTabs = new ArrayList<>();

    @Setter
    private DefaultMutableTreeNode treeRoot;
    @Setter
    private JTree tree;
    @Getter
    private JPanel panel;

    private final boolean lightEdit;

    public DConnection(Project project, BenchUI benchUI) {
        this.lightEdit = false;
        this.benchUI = benchUI;
        this.project = project;
    }

    public DConnection(Project project, BenchUI benchUI, boolean lightEdit) {
        this.lightEdit = lightEdit;
        this.benchUI = benchUI;
        this.project = project;
    }

    public void createNewConnectionTab() {
        panel = new JPanel(new BorderLayout());
        benchUI.getFrame().setCursor(Cursor.getDefaultCursor());

        workTabs = new JTabbedPane();

        workTabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    int tabIndex = workTabs.indexAtLocation(e.getX(), e.getY());
                    if (tabIndex >= 1) {
                        workTabs.removeTabAt(tabIndex);
                        editorTabs.remove(tabIndex);
                    }
                }
            }
        });

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        //toolBar.add(new JButton("Connect"));
        JButton btnNewQuery = new JButton("New Query");
        btnNewQuery.addActionListener(e -> {
            addEditorTab("/* Your query goes here */");
        });
        toolBar.add(btnNewQuery);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(translate("connection.tree.title") + getProject().getName() + " (" +
                ((checkConnection()) ? translate("connection.status.connected") : translate("connection.status.disconnected")) + ")");
        setTreeRoot(root);
        setTree(new JTree(root));
        if (!lightEdit) {
            getProjectTree();

            JButton refreshBtn = new JButton(translate("connection.button.refresh"));
            refreshBtn.addActionListener(e -> {
                log.info("Refreshing project tree for {}", getProject().getName());
                getProjectTree();
            });
            toolBar.add(refreshBtn);
            tree.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                        if (path != null) {
                            tree.setSelectionPath(path);
                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                            showPopup(e, node);
                        } else {
                            showPopup(e, null);
                        }
                    }
                }

                private void showPopup(MouseEvent e, DefaultMutableTreeNode node) {
                    JPopupMenu menu;
                    if (node != null) {
                        menu = createContextMenu(node, benchUI);
                    } else {
                        menu = new JPopupMenu();
                        JMenuItem newDB = new JMenuItem(translate("connection.tree.node.database.create"));
                        newDB.addActionListener(a -> addEditorTab("CREATE DATABASE ${name};"));
                        menu.add(newDB);
                    }
                    if (menu != null && menu.getComponentCount() > 0) {
                        menu.show(tree, e.getX(), e.getY());
                    }
                }
            });
        }

        JScrollPane treeScroll = new JScrollPane(tree);

        JSplitPane splitPane =
                new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, workTabs);
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0);

        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        benchUI.addClosableTab(benchUI.getTabbedPane(), getProject().getName(), panel);
        benchUI.getTabbedPane().setSelectedIndex(1);
        benchUI.getMenuBar().enable("file")
                .enable("edit")
                .enable("sql").updateAll();

        addTab(new WelcomeTab());

        FileManager.instance().getProjectData(project, benchUI).forEach(this::addTab);
    }

    public void addCreateTableTab() {
        addTab(new CreateTableTab(this).newTable());
    }

    public void addCreateTableTab(String db, String table) {
        addTab(new CreateTableTab(this).ofRealTable(db, table));
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
                editorTabs.remove(i);
                break;
            }
        }
    }

    public void addTab(IEditorTab tab) {
        JPanel p = tab.getTabComponent(benchUI, this);
        editorTabs.add(tab);
        workTabs.addTab(tab.getTitle(), p);
        workTabs.setSelectedIndex(workTabs.getTabCount() - 1);
    }

    public void handleWindowClosing(JFrame frame) {
        FileManager.instance().save(editorTabs, project);
        boolean unsaved = hasUnsavedChanges();
        if (!unsaved) {
            frame.dispose();
            return;
        }

        for (IEditorTab tab : editorTabs) {
            if (!(tab instanceof EditorTab e)) {
                removeTab(tab);
                continue;
            }
            if (e.isFileSaved()) {
                removeTab(tab);
                continue;
            }

            int option = JOptionPane.showConfirmDialog(
                    frame,
                    "There are unsaved changes in " + e.getTitle() + ". Do you want to save it?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (option == JOptionPane.YES_OPTION) {
                e.saveFile();
            } else if (option == JOptionPane.CANCEL_OPTION) {
                return;
            } else if (option == JOptionPane.NO_OPTION) {
                removeTab(tab);
            }
        }
        frame.dispose();
    }

    public boolean hasUnsavedChanges() {
        boolean unsavedChanges = false;

        for (IEditorTab tab : editorTabs) {
            if (tab instanceof EditorTab e && !e.isFileSaved()) unsavedChanges = true;
        }
        return unsavedChanges;
    }

    public CompletableFuture<Connection> connect() {
        if (lightEdit) return CompletableFuture.completedFuture(null);
        Thread current = Thread.currentThread();
        ClassLoader previous = current.getContextClassLoader();
        current.setContextClassLoader(DriverManagerService.instance().getDriverLoader());

        CompletableFuture<Connection> future = new CompletableFuture<>();
        final String DB_NAME = "jdbc:mysql://"+project.getServer() +"/"+project.getDefaultDatabase()+
                "?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true"
                + (project.isUseSSL() ? "&useSSL=true&requireSSL=true" : "&useSSL=false");

        try {
            // Establish a connection using the provided connection details
            conn = DriverManager.getConnection(DB_NAME, project.getUsername(), project.getPassword());
            future.complete(conn);
        } catch (SQLException ex) {
            // Log any exception that occurs during the connection process
            log.warn("MySQL connection failed: {}", ex.getMessage());
            future.completeExceptionally(ex);
        } finally {
            current.setContextClassLoader(previous);
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
            while (rs.next()) {
                if (rs.getString(1).equalsIgnoreCase("information_schema")) continue;
                if (rs.getString(1).equalsIgnoreCase("performance_schema")) continue;
                if (rs.getString(1).equalsIgnoreCase("mysql")) continue;
                databases.add(rs.getString(1));
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return databases;
    }

    public List<String> getTables(String database) {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement pS = conn.prepareStatement("USE " + database)) {pS.execute();} catch (SQLException e) {}
        try (PreparedStatement pS = conn.prepareStatement("SHOW FULL TABLES IN " + database + " WHERE TABLE_TYPE = 'BASE TABLE'")) {
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
            answer = new SQLAnswer(true, pS.getResultSet(), pS.getUpdateCount(), "Query executed successfully.", -1);
        } catch (SQLException e) {
            log.error(e.getMessage());
            answer = new SQLAnswer(false, null, -1, e.getMessage(), -1);
        }
        return answer;
    }

    public List<String> getViews(String database) {
        List<String> views = new ArrayList<>();
        try (PreparedStatement pS = conn.prepareStatement("SHOW FULL TABLES IN " + database + " WHERE TABLE_TYPE = 'VIEW';")) {
            ResultSet rs = pS.executeQuery();
            while (rs.next()) views.add(rs.getString(1));
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return views;
    }

    public void getProjectTree() {
        benchUI.getFrame().setCursor(Cursor.WAIT_CURSOR);
        log.info("Refreshing project tree for {}", getProject().getName());
        treeRoot.removeAllChildren();
        DefaultMutableTreeNode databases = new DefaultMutableTreeNode(translate("connection.tree.node.database.title"));
        treeRoot.add(databases);
        if (!checkConnection()) return;
        for (String s : getDatabases()) {
            log.info("Adding database {}", s);
            DefaultMutableTreeNode db = new DefaultMutableTreeNode(s);
            databases.add(db);

            DefaultMutableTreeNode tb = new DefaultMutableTreeNode(translate("connection.tree.node.tables.title"));
            DefaultMutableTreeNode views = new DefaultMutableTreeNode("connection.tree.node.views.title");
            DefaultMutableTreeNode procedures = new DefaultMutableTreeNode("connection.tree.node.procedures.title");
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


        DefaultMutableTreeNode jobAgent = new DefaultMutableTreeNode("SQL Agent");
        DefaultMutableTreeNode jobs = new DefaultMutableTreeNode("Jobs");
        DefaultMutableTreeNode protocols = new DefaultMutableTreeNode("Protocol");
        DefaultMutableTreeNode errors = new DefaultMutableTreeNode("Errors");
        DefaultMutableTreeNode timetables = new DefaultMutableTreeNode("Time Tables");
        jobAgent.add(jobs);
        jobAgent.add(protocols);
        jobAgent.add(errors);
        jobAgent.add(timetables);
        treeRoot.add(jobAgent);

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                          boolean selected, boolean expanded,
                                                          boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                if (value instanceof DefaultMutableTreeNode node) {
                    Object userObject = node.getUserObject();
                    if ("SQL Agent".equals(userObject)) {
                        setIcon(new ImageIcon(getClass().getResource("/icons/app/agent.png")));
                    } else if (translate("connection.tree.node.tables.title").equals(userObject)) {
                        setIcon(new ImageIcon(getClass().getResource("/icons/app/folder.png")));
                    } else if (translate("connection.tree.node.views.title").equals(userObject)) {
                        setIcon(new ImageIcon(getClass().getResource("/icons/app/folder.png")));
                    } else if (translate("connection.tree.node.procedures.title").equals(userObject)) {
                        setIcon(new ImageIcon(getClass().getResource("/icons/app/folder.png")));
                    } else if (translate("connection.tree.node.database.title").equals(userObject)) {
                        setIcon(new ImageIcon(getClass().getResource("/icons/app/folder.png")));
                    }
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                    if (parent != null) {
                        Object parentObject = parent.getUserObject();
                        if (translate("connection.tree.node.database.title").equals(parentObject.toString())) {
                            setIcon(new ImageIcon(getClass().getResource("/icons/app/schema.png")));
                        } else if (translate("connection.tree.node.tables.title").equals(parentObject.toString())) {
                            setIcon(new ImageIcon(getClass().getResource("/icons/app/table.png")));
                        } else if (translate("connection.tree.node.views.title").equals(parentObject.toString())) {
                            setIcon(new ImageIcon(getClass().getResource("/icons/app/view.png")));
                        } else if ("SQL Agent".equals(parentObject.toString())) {
                            setIcon(new ImageIcon(getClass().getResource("/icons/app/folder.png")));
                        }
                    }
                }

                return this;
            }
        });

        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.reload();
        tree.expandPath(new TreePath(databases.getPath()));
        benchUI.getFrame().setCursor(Cursor.DEFAULT_CURSOR);
    }

    public JPopupMenu createContextMenu(DefaultMutableTreeNode node, BenchUI ui) {
        Object userObject = node.getUserObject();
        String name = userObject.toString();

        JPopupMenu menu = new JPopupMenu();

        if (name.equals("Databases")) {
            JMenuItem refresh = new JMenuItem("Refresh");
            refresh.addActionListener(e -> {
                getProjectTree();
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
                int result = JOptionPane.showConfirmDialog(ui.getFrame(), "Do you really want to drop (delete) this schema? This cannot be undone!");
                if (result == JOptionPane.YES_OPTION) {
                    EditorTab t = addEditorTab("DROP DATABASE `" + dbName + "`;");
                    t.execute();
                }
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
            JMenuItem select = new JMenuItem("Select Rows");
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
            drop.addActionListener(e -> {
                String tableName = node.getUserObject().toString();
                String db = ((DefaultMutableTreeNode) node.getParent().getParent()).getUserObject().toString();
                EditorTab t = addEditorTab("DROP TABLE `" + db + "." + tableName + "`;");
                int result = JOptionPane.showConfirmDialog(ui.getFrame(), "Do you really want to drop (delete) this table? This cannot be undone!");
                if (result == JOptionPane.YES_OPTION) t.execute();
            });
            JMenuItem truncate = new JMenuItem("Truncate Table ");
            truncate.addActionListener(e -> {
                String tableName = node.getUserObject().toString();
                String db = ((DefaultMutableTreeNode) node.getParent().getParent()).getUserObject().toString();
                EditorTab t = addEditorTab("TRUNCATE TABLE `" + db + "." + tableName + "`;");
                int result = JOptionPane.showConfirmDialog(ui.getFrame(), "Do you really want to truncate this table? This cannot be undone!");
                if (result == JOptionPane.YES_OPTION) t.execute();
            });
            JMenuItem alter = new JMenuItem("Alter Table ");
            alter.addActionListener(e -> {
                String tableName = node.getUserObject().toString();
                String db = ((DefaultMutableTreeNode) node.getParent().getParent()).getUserObject().toString();
                addCreateTableTab(db, tableName);
            });
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
            menu.add(truncate);
            menu.add(alter);
            menu.add(generatorMenu);
        }

        else if (name.equals("Views")) {
            JMenuItem create = new JMenuItem("Create new View");
            menu.add(create);
        }

        else if (node.getParent() != null &&
                ((DefaultMutableTreeNode) node.getParent()).getUserObject().equals("Views")) {
            JMenuItem browse = new JMenuItem("Edit View ");
            JMenuItem select = new JMenuItem("Select data");
            JMenuItem drop = new JMenuItem("Drop View ");
            menu.add(browse);
            menu.add(select);
            menu.add(drop);
        }

        else if (name.equals("Procedures")) {
            JMenuItem create = new JMenuItem("Create new Procedure");
            menu.add(create);
        }

        return menu;
    }

    public TableDefinition getTableDefinition(String database, String table) {

        TableDefinition def = new TableDefinition();
        def.setTableName(table);

        String sql = "SHOW FULL COLUMNS FROM `" + database + "`.`" + table + "`;";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String name = rs.getString("Field");
                String typeRaw = rs.getString("Type");
                String key = rs.getString("Key");
                String extra = rs.getString("Extra");
                String nullable = rs.getString("Null");

                String type;
                Integer length = null;

                int idx = typeRaw.indexOf('(');
                if (idx > 0) {
                    type = typeRaw.substring(0, idx).toUpperCase();
                    length = Integer.parseInt(
                            typeRaw.substring(idx + 1, typeRaw.indexOf(')', idx))
                    );
                } else {
                    type = typeRaw.toUpperCase();
                }

                boolean primaryKey = "PRI".equalsIgnoreCase(key);
                boolean notNull = "NO".equalsIgnoreCase(nullable);
                boolean autoIncrement = extra != null && extra.toLowerCase().contains("auto_increment");

                TableColumn column = new TableColumn(
                        name,
                        type,
                        length,
                        primaryKey,
                        notNull,
                        autoIncrement
                );

                def.addColumn(column);
            }

        } catch (SQLException e) {
            log.error("Failed to load table structure for {}.{}",
                    database, table, e);
        }

        return def;
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

    public record SQLAnswer(boolean success, ResultSet resultSet, int updateCount, String message, long executionTimeMs) {

    }
}
