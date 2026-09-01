package de.julianweinelt.datacat.dbx.api.ui.tree;

import java.util.ArrayList;
import java.util.List;

public class ExplorerEventBus {

    private final List<TreeListener> listeners = new ArrayList<>();

    public void register(TreeListener listener) {
        listeners.add(listener);
    }

    public void fireSelected(TreeNode node) {
        listeners.forEach(l -> l.onNodeSelected(node));
    }

    public void fireDoubleClick(TreeNode node) {
        listeners.forEach(l -> l.onNodeDoubleClicked(node));
    }
}