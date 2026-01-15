package de.julianweinelt.databench.dbx.backup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDefinition {

    private String name;

    /**
     * SQL type as string, e.g.:
     * INT
     * VARCHAR(255)
     * BIGINT
     * TEXT
     */
    private String type;

    private boolean nullable;

    private boolean autoIncrement;

    /**
     * Optional default value (as string representation)
     */
    private String defaultValue;
}
