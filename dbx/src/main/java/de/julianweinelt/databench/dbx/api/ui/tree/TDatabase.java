package de.julianweinelt.databench.dbx.api.ui.tree;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class TDatabase extends TreeBase implements TreeCallback<TDatabase> {
    private boolean offline;

    private final List<TTable> tables = new ArrayList<>();
    private final List<TView> views = new ArrayList<>();

    public TDatabase(UUID uniqueID, String name, boolean offline) {
        super(uniqueID, name);
        this.offline = offline;
    }
    public TDatabase (String name) {
        super(UUID.randomUUID(), name);
        offline = false;
    }

    @Override
    public TreeObject createTreeObject() {
        return new TreeObject(TreeObject.Type.DATABASE, getUniqueID(), getName()) {
            @Override
            public String icon() {
                return (offline ? "/icons/editor/database-offline.png" : "/icons/editor/database.png");
            }
        };
    }

    @Override
    public TDatabase call() {
        return this;
    }
}