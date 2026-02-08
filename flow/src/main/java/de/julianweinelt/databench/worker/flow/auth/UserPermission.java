package de.julianweinelt.databench.worker.flow.auth;

import lombok.Getter;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class UserPermission {
    private final UUID userId;
    private final HashMap<String, PermissionType> permissions = new HashMap<>();

    public UserPermission(UUID userId) {
        this.userId = userId;
    }

    public enum PermissionType {
        READ, WRITE, READ_WRITE
    }


}
