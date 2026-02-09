package de.julianweinelt.databench.flow.storage;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Configuration {
    @Getter(AccessLevel.NONE)
    private String INFO = "WARNING: DO NOT CHANGE THIS FILE MANUALLY!!!! USE THE DATABENCH EDITOR!";

    private boolean ignoreSafeMode = false;
    private String dbHost = "localhost";
    private int dbPort = 3306;
    private String dbUser = "root";
    private String dbPassword = "";

    private final List<String> jvmArgs = new ArrayList<>();

    private int tokenLifetime = 3600;
    private String jwtSecret = "";
    private String tokenClaim = UUID.randomUUID().toString();

    private String encryptionPassword = "";

    private int internalSocketPort = 47386;
    private int internalServerPort = 47387;
    private String internalServerAddress = "0.0.0.0";
}