package de.julianweinelt.databench.api;

import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.ui.BenchUI;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class DConnection {
    private final Project project;
    private Connection conn;
    @Setter
    private JTabbedPane workTabs;

    public DConnection(Project project) {
        this.project = project;
    }


    public CompletableFuture<Connection> connect() {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        final String DB_NAME = "jdbc:mysql://"+project.getServer()+"/"+project.getDefaultDatabase()+"?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true";

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
        return true;
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

    public void getProjectTree(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode databases = new DefaultMutableTreeNode("Databases");
        root.add(databases);
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
            JMenuItem refresh = new JMenuItem("Refresh Databases");
            refresh.addActionListener(e -> {

            });
            JMenuItem newDB = new JMenuItem("New Database");
            menu.add(refresh);
            menu.add(newDB);
        }

        else if (node.getParent() != null &&
                ((DefaultMutableTreeNode) node.getParent()).getUserObject().equals("Databases")) {
            JMenuItem drop = new JMenuItem("Drop Database");
            menu.add(drop);
        }

        else if (name.equals("Tables")) {
            JMenuItem create = new JMenuItem("Create new Table");
            create.addActionListener(e -> {
                ui.addCreateTableEditorTab(workTabs, "New Table");
            });
            menu.add(create);
        }

        else if (node.getParent() != null &&
                ((DefaultMutableTreeNode) node.getParent()).getUserObject().equals("Tables")) {
            JMenuItem edit = new JMenuItem("Edit Table ");
            JMenuItem select = new JMenuItem("Select data");
            JMenuItem drop = new JMenuItem("Drop Table ");
            menu.add(edit);
            menu.add(select);
            menu.add(drop);
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

}
