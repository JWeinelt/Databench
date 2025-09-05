package de.julianweinelt.databench.data;

import de.julianweinelt.databench.DataBench;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Configuration {
    private List<Project> projects = new ArrayList<>();
    private final String clientVersion = "1.0.0";

    public void addProject(Project project) {
        projects.add(project);
    }

    public static Configuration getConfiguration() {
        return DataBench.getInstance().getConfigManager().getConfiguration();
    }
}