package de.julianweinelt.databench.dbx.api.ui.tree;

import lombok.Getter;
import lombok.Setter;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.UUID;

@Getter @Setter
public abstract class TreeObject {
    private final UUID uniqueID;
    private final Type type;
    private String name;

    public abstract String icon();

    public DefaultMutableTreeNode getNode() {
        return new DefaultMutableTreeNode(this);
    }

    public TreeObject(Type type, UUID uniqueID, String name) {
        this.uniqueID = uniqueID;
        this.type = type;
        this.name = name;
    }

    public enum Type {
        PROJECT,
        DATABASE,
        FOLDER_TABLES,
        FOLDER_VIEWS,
        FOLDER_PROCEDURES,
        FOLDER_FUNCTIONS,
        TABLE,
        VIEW,
        PROCEDURE,
        FUNCTION,
        JOB // SQL Server only
    }
}
