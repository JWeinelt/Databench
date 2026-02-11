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

    public static JobAgent instance() {
        return instance;
    }
    public JobAgent() {
        instance = this;
        jobFolder = new File("jobs");
        if (jobFolder.mkdirs()) log.debug("Jobs folder created");
    }
}