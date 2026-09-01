package de.julianweinelt.datacat.dbx.api.ui.tree;

import java.util.List;

public interface NodeChildrenProvider {

    boolean supports(TreeNode node);

    List<TreeNode> loadChildren(TreeNode node);
}