package de.julianweinelt.databench.worker.job;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class JobStepImpl {
    private final UUID jobId;
    private final UUID uniqueId = UUID.randomUUID();
    private int priority;
    private StepOperation operation;
    private String name;
    private String description;

    public JobStepImpl(UUID jobId) {
        this.jobId = jobId;
    }

    public enum StepOperation {
        GO_FURTHER,
        GO_TO,
        END_CLEAR,
        END_ERROR
    }
}
