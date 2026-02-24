package de.julianweinelt.databench.dbx.api.ui.tree;

import java.util.UUID;

public record TableRelation(String name, UUID table1, UUID table2, String table1Column, String table2Column) {}