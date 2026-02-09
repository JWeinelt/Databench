package de.julianweinelt.databench.flow.job;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class JobAgent {
    private final Gson GSON = new Gson();
    private static JobAgent instance;
    private final File jobFolder;

    private final List<Job> jobs = new ArrayList<>();

    public static JobAgent instance() {
        return instance;
    }
    public JobAgent() {
        instance = this;
        jobFolder = new File("jobs");
        if (jobFolder.mkdirs()) log.debug("Jobs folder created");
    }

    public Optional<Job> getJob(UUID uuid) {
        return jobs.stream().filter(j -> j.getUniqueId().equals(uuid)).findFirst();
    }

    public JsonArray getMinimalJobData() {
        JsonArray array = new JsonArray();
        jobs.forEach(j -> {
            JsonObject o = new JsonObject();
            o.addProperty("name", j.getName());
            o.addProperty("uniqueId", j.getUniqueId().toString());
            array.add(o);
        });
        return array;
    }

    public void save() {
        for (Job j : jobs) {
            try (FileWriter w = new FileWriter(new File(jobFolder, j.getUniqueId() + ".json"))) {
                w.write(GSON.toJson(j));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void load() {
        jobs.clear();
        File[] files = jobFolder.listFiles();
        if (files == null) return;
        for (File f : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                Job job = GSON.fromJson(br, Job.class);
                jobs.add(job);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}