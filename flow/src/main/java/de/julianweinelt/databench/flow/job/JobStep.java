package de.julianweinelt.databench.flow.job;

import lombok.Getter;

import java.util.UUID;

@Getter
public class JobStep {
    private final UUID jobId;
    private final UUID uniqueId;
    private int priority;
    private JobStepImpl.StepOperation operation;
    private String name;
    private String description;

    private String script;

    public JobStep(UUID jobId) {
        this.jobId = jobId;
        uniqueId = UUID.randomUUID();
    }
    public JobStep(JobStepImpl impl, String script) {
        this.jobId = impl.getJobId();
        uniqueId = impl.getUniqueId();
        this.priority = impl.getPriority();
        this.operation = impl.getOperation();
        this.name = impl.getName();
        this.description = impl.getDescription();

        this.script = script;
    }
}
