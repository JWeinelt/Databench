package de.julianweinelt.databench.launcher.storage;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Configuration {
    private final List<String> jvmArgs = new ArrayList<>();
    private int maxMemMB = 1024;
}