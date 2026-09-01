package de.julianweinelt.datacat.dbx.api.ui.tree;

import de.julianweinelt.databench.dbx.database.ADatabase;

public class ProjectTreeBuilder {

    private final ADatabase database;
    private final NodeStructureRegistry structureRegistry;

    public ProjectTreeBuilder(ADatabase database,
                              NodeStructureRegistry structureRegistry) {
        this.database = database;
        this.structureRegistry = structureRegistry;
    }

    public TreeNode build() {

        TreeNode root = new TreeNode(
                DefaultNodeTypes.PROJECT,
                "Project",
                null
        );

        TreeNode dbRoot = new TreeNode(
                DefaultNodeTypes.DATABASES,
                "Databases",
                null
        );

        root.addChild(dbRoot);

        for (String dbName : database.getDatabases()) {

            TreeNode dbNode = new TreeNode(
                    DefaultNodeTypes.DATABASE,
                    dbName,
                    dbName
            );

            dbRoot.addChild(dbNode);

            loadDatabaseChildren(dbNode, dbName);
        }

        return root;
    }

    private void loadDatabaseChildren(TreeNode dbNode, String dbName) {

        TreeNode tablesFolder = new TreeNode(
                DefaultNodeTypes.TABLES,
                "Tables",
                null
        );

        dbNode.addChild(tablesFolder);

        for (String table : database.getTables(dbName)) {
            tablesFolder.addChild(
                    new TreeNode(DefaultNodeTypes.TABLE, table, table)
            );
        }

        structureRegistry.loadAdditionalChildren(dbNode);
    }
}