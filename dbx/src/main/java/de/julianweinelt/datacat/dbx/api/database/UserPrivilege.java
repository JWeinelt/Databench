package de.julianweinelt.datacat.dbx.api.database;

import java.util.Map;
import java.util.Set;

public record UserPrivilege(
        String username,
        String host,
        Set<String> globalPrivileges,
        boolean grantOption,
        Map<String, Set<String>> databasePrivileges,
        Map<String, Set<String>> tablePrivileges
) {

}
