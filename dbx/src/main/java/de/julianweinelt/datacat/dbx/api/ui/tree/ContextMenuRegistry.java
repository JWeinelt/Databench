package de.julianweinelt.datacat.dbx.api.ui.tree;

import java.util.ArrayList;
import java.util.List;

public class ContextMenuRegistry {

    private final List<ContextMenuContributor> contributors = new ArrayList<>();

    public void register(ContextMenuContributor contributor) {
        contributors.add(contributor);
    }

    public List<TreeContextAction> getActions(TreeNode node) {
        return contributors.stream()
                .filter(c -> c.supports(node))
                .flatMap(c -> c.getActions(node).stream())
                .toList();
    }
}