package de.julianweinelt.datacat.server.model;

import de.julianweinelt.datacat.server.store.account.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class MAccount {
    @Getter
    private static final List<MAccount> accounts = new ArrayList<>();

    private final UUID uniqueID;
    private final String username;
    private final String displayName;

    public MAccount(Account account) {
        uniqueID = account.getUniqueId();
        username = account.getUsername();
        displayName = "";
        register();
    }

    public MAccount(Account account, String displayName) {
        uniqueID = account.getUniqueId();
        username = account.getUsername();
        this.displayName = displayName;
        register();
    }

    public MAccount(UUID uniqueID, String username, String displayName) {
        this.uniqueID = uniqueID;
        this.username = username;
        this.displayName = displayName;
        register();
    }

    private void register() {
        accounts.add(this);
    }

    public static MAccount get(UUID uniqueID) {
        return accounts.stream().filter(a -> a.uniqueID.equals(uniqueID)).findFirst().orElse(null);
    }
    public static MAccount get(String username) {
        return accounts.stream().filter(a -> a.username.equals(username)).findFirst().orElse(null);
    }
}
