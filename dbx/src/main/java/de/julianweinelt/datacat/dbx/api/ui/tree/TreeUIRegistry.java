package de.julianweinelt.datacat.dbx.api.ui.tree;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class TreeUIRegistry {

    private final Map<TreeNodeType, IconProvider> iconProviders = new HashMap<>();

    public void registerIcon(TreeNodeType type, IconProvider provider) {
        iconProviders.put(type, provider);
    }

    public Icon resolveIcon(TreeNode node) {
        return iconProviders.getOrDefault(node.getType(), n -> null).getIcon(node);
    }
}