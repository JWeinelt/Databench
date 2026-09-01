package de.julianweinelt.datacat.dbx.api.ui.tree;

import java.util.List;

public interface ContextMenuContributor {

    boolean supports(TreeNode node);

    void contribute(TreeNode node);

    List<TreeContextAction> getActions(TreeNode node);
}