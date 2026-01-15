package de.julianweinelt.databench.dbx.backup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexDefinition {

    private String name;

    private List<String> columns;

    private boolean unique;
}
