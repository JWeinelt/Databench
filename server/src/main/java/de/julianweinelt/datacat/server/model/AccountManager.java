package de.julianweinelt.datacat.server.model;

import de.julianweinelt.datacat.server.storage.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountManager {
    private final List<Account> loadedAccounts = new ArrayList<>();


    /**
     * Gets an {@link Account} object for the matching UUID.
     * When
     * @param uniqueId
     * @return
     */
    public Account getAccount(UUID uniqueId) {
        for (Account a : loadedAccounts) {
            if (a.getUniqueID().equals(uniqueId)) return a;
        }

        Account a = loadAccount(uniqueId);
        loadedAccounts.add(a);
        return a;
    }

    private Account loadAccount(UUID uniqueId) {
        return Database.instance().loadAccount(uniqueId);
    }
}