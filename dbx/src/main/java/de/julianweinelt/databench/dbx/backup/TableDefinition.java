package de.julianweinelt.databench.dbx.backup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDefinition {

    private String name;

    private String engine;

    private List<ColumnDefinition> columns;

    private List<String> primaryKey;

    private List<IndexDefinition> indexes;

    public ColumnDefinition getColumn(String name) {
        return columns.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }
}
