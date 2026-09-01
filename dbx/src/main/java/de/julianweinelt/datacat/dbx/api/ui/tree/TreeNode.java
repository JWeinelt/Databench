package de.julianweinelt.datacat.dbx.api.ui.tree;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TreeNode {

    private final TreeNodeType type;
    private final String label;
    private final Object value;
    private final List<TreeNode> children = new ArrayList<>();

    public TreeNode(TreeNodeType type, String label, Object value) {
        this.type = type;
        this.label = label;
        this.value = value;
    }

    public void addChild(TreeNode node) {
        children.add(node);
    }
}