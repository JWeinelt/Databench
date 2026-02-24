package de.julianweinelt.databench.dbx.api.ui.tree;

import lombok.Getter;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class ProjectTree {
    private final UUID project;
    private final List<TDatabase> databases = new ArrayList<>();

    public ProjectTree(UUID project) {
        this.project = project;
    }

    public void addDatabase(TDatabase db) {
        databases.add(db);
    }

    public DefaultMutableTreeNode render() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Project");
        for (TDatabase db : databases) {
            root.add(db.createTreeObject().getNode());
        }
        return root;
    }
}