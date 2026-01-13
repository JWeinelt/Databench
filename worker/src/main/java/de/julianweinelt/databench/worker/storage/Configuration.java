package de.julianweinelt.databench.worker.storage;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    private int internalSocketPort = 47386;
}