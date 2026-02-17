package de.julianweinelt.databench.dbx.api.ui.tree;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public abstract class TreeBase {
    private final UUID uniqueID;
    private String name;

    public TreeBase(UUID uniqueID, String name) {
        this.uniqueID = uniqueID;
        this.name = name;
    }

    public abstract TreeObject createTreeObject();
}
