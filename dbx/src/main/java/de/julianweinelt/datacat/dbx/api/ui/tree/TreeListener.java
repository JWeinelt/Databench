package de.julianweinelt.datacat.dbx.api.ui.tree;

public interface TreeListener {

    void onNodeSelected(TreeNode node);

    void onNodeDoubleClicked(TreeNode node);
}