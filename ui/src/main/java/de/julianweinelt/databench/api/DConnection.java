package de.julianweinelt.databench.api;

import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.dbx.api.drivers.DriverManagerService;
import de.julianweinelt.databench.dbx.database.DatabaseMetaData;
import de.julianweinelt.databench.dbx.database.DatabaseRegistry;
import de.julianweinelt.databench.ui.BenchUI;
import de.julianweinelt.databench.ui.editor.*;
import de.julianweinelt.databench.ui.flow.FlowUI;
import de.julianweinelt.databench.util.FileUtil;
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
import java.io.File;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.julianweinelt.databench.dbx.util.LanguageManager.translate;

@Slf4j
@Getter
@SuppressWarnings({"SqlSourceToSinkFlow", "SqlResolve"})
public class DConnection implements IFileWatcherListener {
    private final Pattern PLACEHOLDER_PATTERN =
        Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");
    private final Project project;
    private final BenchUI benchUI;
    private Connection conn;
    @Setter
    private JTabbedPane workTabs;
    private final List<IEditorTab> editorTabs = new ArrayList<>();

    @Setter
    private DefaultMutableTreeNode treeRoot;
    @Setter
    private JTree tree;
    @Setter
    private JTree fileTree;

    private FileWatcher fileWatcher;

    @Setter
    private DefaultMutableTreeNode fileTreeRoot;

    @Getter
    private JPanel panel;

    private final boolean lightEdit;
    private final DatabaseMetaData databaseTypeMeta;

    public DConnection(Project project, BenchUI benchUI) {
        this.lightEdit = false;
        this.benchUI = benchUI;
        this.project = project;
        databaseTypeMeta = DatabaseRegistry.instance().getMeta(project.getDatabaseType());
    }

    public DConnection(Project project, BenchUI benchUI, boolean lightEdit) {
        this.lightEdit = lightEdit;
        this.benchUI = benchUI;
        this.project = project;
        databaseTypeMeta = DatabaseRegistry.instance().getMeta(project.getDatabaseType());
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
        btnNewQuery.addActionListener(e -> addEditorTab("/* Your query goes here */"));
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
                        JMenuItem noObj = new JMenuItem("Please select an object...");
                        noObj.setEnabled(false);
                        menu.add(noObj);
                    }
                    if (menu != null && menu.getComponentCount() > 0) {
                        menu.show(tree, e.getX(), e.getY());
                    }
                }
            });
        }

        JScrollPane projectTreeScroll = new JScrollPane(tree);

        createProjectFolder();
        JScrollPane fileTreeScroll = new JScrollPane(fileTree);

        JPanel flowPanel = new JPanel();

        JTabbedPane leftTabs = new JTabbedPane();
        if (!lightEdit) leftTabs.addTab(translate("project.tabs.database"), projectTreeScroll);
        leftTabs.addTab(translate("project.tabs.files"), fileTreeScroll);
        leftTabs.addTab(translate("project.tabs.flow"), new FlowUI().createFlowLoginPanel());

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftTabs,
                workTabs
        );
        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0);

        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        benchUI.addClosableTab(
                benchUI.getTabbedPane(),
                getProject().getName(),
                panel
        );

        benchUI.getTabbedPane().setSelectedIndex(1);
        benchUI.getMenuBar()
                .enable("file", "edit", "sql")
                .updateAll();

        addTab(new WelcomeTab());

        FileManager.instance().getProjectData(project, benchUI).forEach(this::addTab);
    }

    private void createProjectFolder() {
        //TODO: Use another folder in users home
        File folder = new File("projects", project.getUuid().toString());
        if (folder.mkdirs()) log.debug("Created workspace folder for project {}", project.getName());
        fileWatcher = new FileWatcher(folder, this);
        fileTreeRoot = new DefaultMutableTreeNode();
        fileWatcher.scanTree();
        fileTree = new JTree(fileTreeRoot);
        createFileSystemContextMenus();
        updateProjectFileTree();
    }

    private void updateProjectFileTree() {
        fileTreeRoot.removeAllChildren();
        fileWatcher.createTree(fileTreeRoot);

        fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                          boolean selected, boolean expanded,
                                                          boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                if (value instanceof DefaultMutableTreeNode node) {
                    Object user = node.getUserObject();
                    if (user instanceof FileObject file) {
                        setText(file.name());
                        setIcon(getIconForType(file.type()));
                    }
                }

                return this;
            }
        });

        DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();
        model.reload();
    }

    private void createFileSystemContextMenus() {
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        fileTree.setSelectionPath(path);
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        showPopup(e, node);
                    } else {
                        showPopup(e, null);
                    }
                } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        FileObject o = (FileObject) node.getUserObject();
                        if (o.directory()) {
                            fileTree.expandPath(path);
                        } else
                            openFileFromFileSystem(o);
                    }

                }
            }

            private void showPopup(MouseEvent e, DefaultMutableTreeNode node) {
                JPopupMenu menu;
                if (node != null) {
                    menu = createFileContext(node, benchUI);
                } else {
                    menu = new JPopupMenu();
                    JMenuItem noObj = new JMenuItem("Please select an object...");
                    noObj.setEnabled(false);
                    menu.add(noObj);
                }
                if (menu.getComponentCount() > 0) {
                    menu.show(fileTree, e.getX(), e.getY());
                }
            }
        });
    }

    private JPopupMenu createFileContext(DefaultMutableTreeNode node, BenchUI ui) {
        JPopupMenu menu = new JPopupMenu();
        JMenu newMenu = new JMenu("New");
        newMenu.add(new JMenuItem("File"));
        newMenu.add(new JMenuItem("Directory"));
        newMenu.addSeparator();
        newMenu.add(new JMenuItem("SQL Query File"));
        newMenu.add(new JMenuItem("CSV from Query"));
        newMenu.add(new JMenuItem("XLSX File"));
        newMenu.add(new JMenuItem("JSON File"));

        menu.add(newMenu);

        menu.addSeparator();

        menu.add(new JMenuItem("Cut"));
        menu.add(new JMenuItem("Copy"));
        menu.add(new JMenuItem("Copy Path/Reference..."));
        menu.add(new JMenuItem("Paste"));
        menu.addSeparator();
        menu.add(new JMenuItem("Rename"));
        menu.addSeparator();
        JMenu openIn = new JMenu("Open In");
        openIn.add(new JMenuItem("Open In Explorer"));
        openIn.add(new JMenuItem("Open In Associated Application"));
        openIn.add(new JMenuItem("Open In..."));
        menu.add(openIn);

        JMenu gitMenu = new JMenu("Git");
        gitMenu.add(new JMenuItem("Coming soon..."));
        menu.add(gitMenu);
        return menu;
    }

    private Icon getIconForType(FileType type) {
        return loadIcon("/icons/editor/files/" + type.displayFile + ".png", 20);
    }

    public void handleFileEvent(File file) {
        if (file.getName().endsWith(".sql")) {
            String content = FileUtil.readFile(file);
            addEditorTab(content);
        } else {
            log.warn("Unknown file type: {}", file.getName());
            int val = JOptionPane.showConfirmDialog(benchUI.getFrame(), "Unknown file type: " + file.getName()
                    + "\n\nShould DataBench try to load it anyway?", "File type mismatch", JOptionPane.YES_NO_OPTION);
            if (val == JOptionPane.YES_OPTION) {
                addEditorTab(FileUtil.readFile(file));
            }
        }
    }

    public void addCreateTableTab() {
        addTab(new CreateTableTab(this).newTable());
    }

    public void addCreateViewTab() {
        addTab(new CreateViewTab(this).newTable());
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
        } else if (content.contains("${")) {
            EditorTab tab = new EditorTab(this, "-- You've entered an invalid value.", benchUI);
            addTab(tab);
            return tab;
        }


        EditorTab tab = new EditorTab(this, content, benchUI);
        addTab(tab);
        return tab;
    }

    public void disconnect() {
        log.info("Disconnecting from {}", getProject().getName());
        if (conn != null) {
            try {
                conn.close();
                log.info("Successfully disconnected from {}", getProject().getName());
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }
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

    public void updateTitle(IEditorTab tab) {
        int idx = 0;
        for (IEditorTab t : editorTabs) {
            if (tab.getId().equals(t.getId())) {
                log.debug("Updating title at {} to {}", idx, t.getTitle());
                workTabs.setTitleAt(idx, t.getTitle());
                break;
            }
            idx++;
        }
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
                e.saveFile(false);
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

        log.info("Project {} is {}", project.getUuid(), databaseTypeMeta.engineName());

        CompletableFuture<Connection> future = new CompletableFuture<>();
        String DB_NAME = databaseTypeMeta.jdbcURL().replace("${server}", project.getServer())
                .replace("${database}", project.getDefaultDatabase())
                .replace("${parameters}", databaseTypeMeta.parameters(databaseTypeMeta.defaultParameters().build()));

        try {
            // Establish a connection using the provided connection details
            if (!project.getDatabaseType().equalsIgnoreCase("mssql")) {
                conn = DriverManager.getConnection(DB_NAME, project.getUsername(), project.getPassword());
                future.complete(conn);
            } else {
                conn = DriverManager.getConnection(DB_NAME);
                future.complete(conn);
            }
        } catch (SQLException ex) {
            // Log any exception that occurs during the connection process
            log.warn("SQL connection failed: {}", ex.getMessage());
            if (project.getDatabaseType().equalsIgnoreCase("mssql")) {
                log.warn("SQL Server (Windows Auth) connection failed: {}", ex.getMessage());
            }
            future.completeExceptionally(ex);
        } finally {
            current.setContextClassLoader(previous);
        }
        return future;
    }

    public boolean testConnection() {
        try {
            return !connect().get().isClosed();
        } catch (Exception e) {
            log.error("Failed to connect to database");
            log.error(e.getMessage());
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

    public List<DBObject> getDatabases() {
        List<DBObject> databases = new ArrayList<>();
        try (PreparedStatement pS = conn.prepareStatement(databaseTypeMeta.syntax().showDatabases())) {
            ResultSet rs = pS.executeQuery();
            while (rs.next()) {
                if (project.getDatabaseType().equalsIgnoreCase("mssql")) {
                    DBObject object = new DBObject(rs.getString(1), rs.getString(2).equals("OFFLINE"));
                    databases.add(object);
                } else {
                    if (rs.getString(1).equalsIgnoreCase("information_schema")) continue;
                    if (rs.getString(1).equalsIgnoreCase("performance_schema")) continue;
                    if (rs.getString(1).equalsIgnoreCase("mysql")) continue;
                    DBObject object = new DBObject(rs.getString(1), false);
                    databases.add(object);
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return databases;
    }

    public List<String> getTables(String database) {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement pS = conn.prepareStatement("USE " + database)) {pS.execute();} catch (SQLException ignored) {}
        try (PreparedStatement pS = conn.prepareStatement(databaseTypeMeta.syntax().showTables().replace("${db}", database))) {
            ResultSet rs = pS.executeQuery();
            while (rs.next()) tables.add(rs.getString(1));
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return tables;
    }

    public SQLAnswer executeSQL(String sql) {
        SQLAnswer answer;
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
        try (PreparedStatement pS = conn.prepareStatement(databaseTypeMeta.syntax().showViews().replace("${db}", database))) {
            ResultSet rs = pS.executeQuery();
            while (rs.next()) views.add(rs.getString(1));
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return views;
    }

    @SuppressWarnings("SqlResolve")
    private List<JobObject> getSQLJobs() {
        if (!project.getDatabaseType().equalsIgnoreCase("mssql")) return new ArrayList<>();
        List<JobObject> jobs = new ArrayList<>();
        try {conn.createStatement().execute("USE msdb;");} catch (SQLException ignored) {}
        // noinspection SqlResolve
        try (PreparedStatement pS = conn.prepareStatement("""
            SELECT
                job_id,
                name,
                enabled,
                date_created,
                date_modified
            FROM dbo.sysjobs
            ORDER BY name;
        """)) {
            ResultSet set = pS.executeQuery();
            log.debug("Retrieved SQL Agent Jobs");
            while (set.next()) {
                jobs.add(new JobObject(set.getString(2), set.getBoolean(3)));
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return jobs;
    }

    public void getProjectTree() {
        benchUI.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        log.debug("Refreshing project tree for {}", getProject().getName());
        treeRoot.removeAllChildren();
        DefaultMutableTreeNode databases = new DefaultMutableTreeNode(translate("connection.tree.node.database.title"));
        treeRoot.add(databases);
        if (!checkConnection()) return;
        for (DBObject s : getDatabases()) {
            log.debug("Adding database {}", s.name);
            DefaultMutableTreeNode db = new DefaultMutableTreeNode(s.name + (s.offline ? " (offline)" : ""));
            databases.add(db);
            if (s.offline) continue;

            DefaultMutableTreeNode tb = new DefaultMutableTreeNode(translate("connection.tree.node.tables.title"));
            DefaultMutableTreeNode views = new DefaultMutableTreeNode(translate("connection.tree.node.views.title"));
            DefaultMutableTreeNode procedures = new DefaultMutableTreeNode(translate("connection.tree.node.procedures.title"));
            db.add(tb);
            for (String t : getTables(s.name)) {
                DefaultMutableTreeNode table = new DefaultMutableTreeNode(t);
                tb.add(table);
            }
            for (String v : getViews(s.name)) {
                DefaultMutableTreeNode view = new DefaultMutableTreeNode(v);
                views.add(view);
            }
            db.add(views);
            db.add(procedures);
        }


        DefaultMutableTreeNode jobAgent = new DefaultMutableTreeNode("SQL Agent");
        DefaultMutableTreeNode jobs = new DefaultMutableTreeNode("Jobs");

        for (JobObject job : getSQLJobs()) {
            DefaultMutableTreeNode jobNode = new DefaultMutableTreeNode(job.name);
            jobs.add(jobNode);
        }

        DefaultMutableTreeNode protocols = new DefaultMutableTreeNode("Protocol");
        DefaultMutableTreeNode errors = new DefaultMutableTreeNode("Errors");
        jobAgent.add(jobs);
        jobAgent.add(protocols);
        jobAgent.add(errors);
        //treeRoot.add(jobAgent);

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                          boolean selected, boolean expanded,
                                                          boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                if (value instanceof DefaultMutableTreeNode node) {
                    Object userObject = node.getUserObject();
                    if ("SQL Agent".equals(userObject)) {
                        setIcon(new ImageIcon(getClassURL("/icons/app/agent.png")));
                    } else if (translate("connection.tree.node.tables.title").equals(userObject)) {
                        setIcon(new ImageIcon(getClassURL("/icons/app/folder.png")));
                    } else if (translate("connection.tree.node.views.title").equals(userObject)) {
                        setIcon(new ImageIcon(getClassURL("/icons/app/folder.png")));
                    } else if (translate("connection.tree.node.procedures.title").equals(userObject)) {
                        setIcon(new ImageIcon(getClassURL("/icons/app/folder.png")));
                    } else if (translate("connection.tree.node.database.title").equals(userObject)) {
                        setIcon(new ImageIcon(getClassURL("/icons/app/folder.png")));
                    }
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                    if (parent != null) {
                        Object parentObject = parent.getUserObject();
                        if (translate("connection.tree.node.database.title").equals(parentObject.toString())) {
                            if (userObject.toString().endsWith("(offline)")) {
                                setIcon(loadIcon("/icons/editor/database-offline.png", 24));
                            } else
                                setIcon(loadIcon("/icons/editor/database.png", 24));
                        } else if (translate("connection.tree.node.tables.title").equals(parentObject.toString())) {
                            setIcon(new ImageIcon(getClassURL("/icons/app/table.png")));
                        } else if (translate("connection.tree.node.views.title").equals(parentObject.toString())) {
                            setIcon(new ImageIcon(getClassURL("/icons/app/view.png")));
                        } else if ("SQL Agent".equals(parentObject.toString())) {
                            setIcon(new ImageIcon(getClassURL("/icons/app/folder.png")));
                        } else if ("Jobs".equals(parentObject.toString())) {
                            setIcon(new ImageIcon(getClassURL("/icons/app/job.png")));
                        }
                    }
                }

                return this;
            }
        });

        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.reload();
        tree.expandPath(new TreePath(databases.getPath()));
        benchUI.getFrame().setCursor(Cursor.getDefaultCursor());
    }

    private Icon loadIcon(String path, int size) {
        ImageIcon icon = new ImageIcon(getClassURL(path));
        Image image = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    public URL getClassURL(String file) {
        return getClass().getResource(file);
    }

    public JPopupMenu createContextMenu(DefaultMutableTreeNode node, BenchUI ui) {
        Object userObject = node.getUserObject();
        String name = userObject.toString();

        JPopupMenu menu = new JPopupMenu();

        if (name.equals(translate("connection.tree.node.database.title"))) {
            JMenuItem refresh = new JMenuItem(translate("connection.button.refresh"));
            refresh.addActionListener(e -> getProjectTree());
            JMenuItem newDB = new JMenuItem(translate("connection.tree.node.database.create"));
            newDB.addActionListener(e -> addEditorTab("CREATE DATABASE ${name};"));
            menu.add(refresh);
            menu.add(newDB);
        } else if (node.getParent() != null &&
                ((DefaultMutableTreeNode) node.getParent()).getUserObject().equals(translate("connection.tree.node.database.title"))) {
            JMenuItem newDB = new JMenuItem(translate("connection.tree.node.database.create"));
            newDB.addActionListener(e -> addEditorTab("CREATE DATABASE ${name};"));
            JMenuItem drop = new JMenuItem("Drop Database");
            drop.addActionListener(e -> {
                String dbName = node.getUserObject().toString();
                int result = JOptionPane.showConfirmDialog(ui.getFrame(), "Do you really want to drop (delete) this schema? This cannot be undone!");
                if (result == JOptionPane.YES_OPTION) {
                    EditorTab t = addEditorTab("DROP DATABASE `" + dbName + "`;");
                    t.execute();
                }
            });
            JMenuItem rename = new JMenuItem("Rename");
            rename.addActionListener(e -> {
                String dbName = node.getUserObject().toString();
                addEditorTab("ALTER DATABASE " + dbName + " MODIFY NAME = ${name}").execute();
            });
            menu.add(newDB);
            menu.add(rename);
            menu.add(drop);
            JMenuItem createTable = new JMenuItem("Create new Table");
            createTable.addActionListener(e -> addCreateTableTab());
            JMenuItem createView = new JMenuItem("Create new View");
            JMenuItem createProcedure = new JMenuItem("Create new Procedure");

            // MSSQL Specific
            if (project.getDatabaseType().equalsIgnoreCase("mssql")) {
                JMenu tasks = new JMenu("Tasks");
                JMenuItem takeOffline = new JMenuItem("Take offline");
                JMenuItem takeOnline = new JMenuItem("Take online");
                if (node.getUserObject().toString().endsWith("(offline)")) {
                    takeOnline.setEnabled(true);
                    takeOffline.setEnabled(false);
                    takeOnline.addActionListener(e -> {
                        String dbName = node.getUserObject().toString();
                        dbName = dbName.substring(0,  dbName.length() - "(offline) ".length());
                        log.info("Taking database {} online", dbName);
                        EditorTab t = addEditorTab("ALTER DATABASE [" + dbName + "] SET ONLINE;");
                        t.execute();
                    });
                } else {
                    takeOnline.setEnabled(false);
                    takeOffline.setEnabled(true);
                    takeOffline.addActionListener(e -> {
                        String dbName = node.getUserObject().toString();
                        EditorTab t = addEditorTab("ALTER DATABASE [" + dbName + "] SET OFFLINE WITH ROLLBACK IMMEDIATE;");
                        t.execute();
                    });
                }
                tasks.add(takeOffline);
                tasks.add(takeOnline);
                menu.add(tasks);
            }
            menu.add(createTable);
            menu.add(createView);
            menu.add(createProcedure);
        }

        else if (name.equals(translate("connection.tree.node.tables.title"))) {
            JMenuItem create = new JMenuItem("Create new Table");
            create.addActionListener(e -> addCreateTableTab());
            menu.add(create);
            JMenuItem refresh = new JMenuItem(translate("connection.button.refresh"));
            refresh.addActionListener(e -> getProjectTree());
            menu.add(refresh);
        }

        else if (node.getParent() != null &&
                ((DefaultMutableTreeNode) node.getParent()).getUserObject().equals(translate("connection.tree.node.tables.title"))) {
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

        else if (name.equals(translate("connection.tree.node.views.title"))) {
            JMenuItem create = new JMenuItem("Create new View");
            create.addActionListener(e -> addCreateViewTab());
            menu.add(create);
        }

        else if (node.getParent() != null &&
                ((DefaultMutableTreeNode) node.getParent()).getUserObject().equals(translate("connection.tree.node.views.title"))) {
            JMenuItem create = new JMenuItem("Create new View");
            create.addActionListener(e -> addCreateViewTab());
            JMenuItem browse = new JMenuItem("Edit View ");
            JMenuItem select = new JMenuItem("Select data");
            JMenuItem drop = new JMenuItem("Drop View ");
            menu.add(create);
            menu.add(browse);
            menu.add(select);
            menu.add(drop);
        }

        else if (name.equals(translate("connection.tree.node.procedures.title"))) {
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
        if (!checkConnection()) connect();

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
                columns[i] = meta.getColumnLabel(i + 1);
            }
            return columns;
        }
    }

    @Override
    public void fileTreeUpdate(FileTree tree) {
        updateProjectFileTree();
    }

    public void openFileFromFileSystem(FileObject o) {
        File f = new File(o.path());
        if (!f.exists()) return;
        String content = FileUtil.readFile(f);
        addTab(new EditorTxtTab(content, benchUI, f, o.type()));
    }

    public record SQLAnswer(boolean success, ResultSet resultSet, int updateCount, String message, long executionTimeMs) {
        public List<String> columns() throws SQLException {
            ResultSetMetaData meta = resultSet.getMetaData();

            int columnCount = meta.getColumnCount();
            String[] columns = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columns[i] = meta.getColumnLabel(i + 1);
            }
            return List.of(columns);
        }
        public Object[][] rows() throws SQLException {
            ResultSetMetaData meta = resultSet.getMetaData();
            int columnCount = meta.getColumnCount();
            List<Object[]> rows = new ArrayList<>();

            while (resultSet.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = resultSet.getObject(i + 1);
                }
                rows.add(row);
            }

            return rows.toArray(new Object[0][]);
        }
    }

    public record DBObject(String name, boolean offline) {}
    public record JobObject(String name, boolean active) {}
}
