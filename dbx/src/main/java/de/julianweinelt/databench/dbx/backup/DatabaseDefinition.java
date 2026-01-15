package de.julianweinelt.databench.dbx.backup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseDefinition {

    private String name;

    private String defaultCharset;
    private String defaultCollation;

    private List<TableDefinition> tables;
}
