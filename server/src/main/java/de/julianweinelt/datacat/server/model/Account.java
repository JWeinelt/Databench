package de.julianweinelt.datacat.server.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class Account {
    private final UUID uniqueID;
    private String eMail;

    private String username;
    private String passwordHashed;

    private long created;
    private long lastActivity;
    private AccountStatus status;
    private VerificationStatus verificationStatus;

    private String aboutMe;

    private final List<String> permissions = new ArrayList<>();
    private final List<Badge> badges = new ArrayList<>();
    private final HashMap<LinkType, String> links = new HashMap<>();

    public Account(UUID uniqueID) {
        this.uniqueID = uniqueID;
    }
}