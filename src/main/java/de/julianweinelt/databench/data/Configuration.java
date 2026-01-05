package de.julianweinelt.databench.data;

import de.julianweinelt.databench.DataBench;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Configuration {
    private String selectedTheme;
    private String locale;

    private String encryptionPassword;

    private boolean checkForUpdates = true;
    private String updateChannel = "stable";

    private final String clientVersion = "1.0.0";

    public static Configuration getConfiguration() {
        return DataBench.getInstance().getConfigManager().getConfiguration();
    }
}