package de.julianweinelt.databench.ui.editor;

import java.util.*;
import java.util.stream.Collectors;

public class SQLBuilder {

    public static String buildCreateOrAlter(TableDefinition table, boolean existing) {
        if (!existing) {
            return buildCreate(table);
        }
        throw new IllegalStateException("Use buildAlter for existing tables");
    }

    public static String buildAlter(TableDefinition original, TableDefinition current) {

        List<String> statements = new ArrayList<>();

        Map<String, TableColumn> oldCols = mapByName(original.getColumns());
        Map<String, TableColumn> newCols = mapByName(current.getColumns());

        for (TableColumn col : newCols.values()) {
            if (!oldCols.containsKey(col.getName())) {
                statements.add("ADD COLUMN " + columnSQL(col));
            } else if (!col.equalsDefinition(oldCols.get(col.getName()))) {
                statements.add("MODIFY COLUMN " + columnSQL(col));
            }
        }

        for (TableColumn col : oldCols.values()) {
            if (!newCols.containsKey(col.getName())) {
                statements.add("DROP COLUMN `" + col.getName() + "`");
            }
        }

        String oldPK = primaryKeySQL(original.getColumns());
        String newPK = primaryKeySQL(current.getColumns());

        if (!Objects.equals(oldPK, newPK)) {
            if (oldPK != null) {
                statements.add("DROP PRIMARY KEY");
            }
            if (newPK != null) {
                statements.add("ADD PRIMARY KEY (" + newPK + ")");
            }
        }

        if (statements.isEmpty()) {
            return "-- No changes";
        }

        return "ALTER TABLE `" + current.getTableName() + "`\n  "
                + String.join(",\n  ", statements)
                + ";";
    }

    private static Map<String, TableColumn> mapByName(List<TableColumn> cols) {
        return cols.stream()
                .collect(Collectors.toMap(TableColumn::getName, c -> c));
    }

    private static String columnSQL(TableColumn c) {
        StringBuilder sb = new StringBuilder();
        sb.append("`").append(c.getName()).append("` ")
          .append(c.getType());

        if (c.getSize() != null) {
            sb.append("(").append(c.getSize()).append(")");
        }
        if (c.isNotNull()) sb.append(" NOT NULL");
        if (c.isAutoIncrement()) sb.append(" AUTO_INCREMENT");

        return sb.toString();
    }

    private static String primaryKeySQL(List<TableColumn> cols) {
        List<String> pkCols = cols.stream()
                .filter(TableColumn::isPrimaryKey)
                .map(c -> "`" + c.getName() + "`")
                .toList();

        return pkCols.isEmpty() ? null : String.join(", ", pkCols);
    }

    private static String buildCreate(TableDefinition table) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE `").append(table.getTableName()).append("` (\n");

        List<String> defs = new ArrayList<>();
        for (TableColumn c : table.getColumns()) {
            defs.add("  " + columnSQL(c));
        }

        String pk = primaryKeySQL(table.getColumns());
        if (pk != null) {
            defs.add("  PRIMARY KEY (" + pk + ")");
        }

        sb.append(String.join(",\n", defs));
        sb.append("\n);");

        return sb.toString();
    }
}
