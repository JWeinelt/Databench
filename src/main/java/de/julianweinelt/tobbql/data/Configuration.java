package de.julianweinelt.tobbql.data;

import de.julianweinelt.tobbql.TobbeBench;
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
        return TobbeBench.getInstance().getConfigManager().getConfiguration();
    }
}