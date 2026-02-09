package de.julianweinelt.databench.flow.job;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
@Setter
public class Job {
    private transient final Gson GSON = new Gson();

    private final UUID uniqueId = UUID.randomUUID();
    private String name;
    private String description;

    private List<JobStep> loadSteps() {
        File[] files = new File("jobs", uniqueId.toString()).listFiles();
        List<JobStep> steps = new ArrayList<>();
        if (files == null) return null;
        for (File f : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                JobStepImpl impl = GSON.fromJson(br, JobStepImpl.class);
                steps.add(new JobStep(impl, loadScript(impl.getUniqueId())));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return steps;
    }

    private String loadScript(UUID stepID) {
        File f = new File(new File("jobs", uniqueId.toString()), stepID + ".sql");
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
                return sb.toString();
            } catch (Exception e) {
                return null;
            }
        } else {
            return "/* SCRIPT NOT FOUND */";
        }
    }
}
