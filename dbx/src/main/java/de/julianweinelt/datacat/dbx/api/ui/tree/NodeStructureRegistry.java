package de.julianweinelt.datacat.dbx.api.ui.tree;

import java.util.ArrayList;
import java.util.List;

public class NodeStructureRegistry {

    private final List<NodeChildrenProvider> providers = new ArrayList<>();

    private final List<ContextMenuContributor> contributors = new ArrayList<>();

    public void register(NodeChildrenProvider provider) {
        providers.add(provider);
    }

    public List<TreeNode> loadChildren(TreeNode node) {
        return providers.stream()
                .filter(p -> p.supports(node))
                .flatMap(p -> p.loadChildren(node).stream())
                .toList();
    }


    public void loadAdditionalChildren(TreeNode node) {
        for (ContextMenuContributor c : contributors) {
            if (c.supports(node)) {
                c.contribute(node);
            }

        }
    }
    }