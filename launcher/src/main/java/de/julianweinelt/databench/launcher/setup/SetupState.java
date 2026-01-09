package de.julianweinelt.databench.launcher.setup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetupState {

    public List<String> selectedLanguages = new ArrayList<>();
    public Set<String> selectedDrivers = new HashSet<>();

    public boolean followSystemTheme = true;
    public String theme = "dark";

    public String javaPath;
    public List<String> jvmArgs = new ArrayList<>();

    public boolean startAfterFinish = true;

}
