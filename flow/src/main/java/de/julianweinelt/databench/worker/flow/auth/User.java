package de.julianweinelt.databench.worker.flow.auth;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter@Setter
public class User {
    private final UUID uniqueId = UUID.randomUUID();

    private String username;
    private String password;

    private long passwordExpiry = -1;
    private UserManager.UserState state = UserManager.UserState.ENABLED;
}