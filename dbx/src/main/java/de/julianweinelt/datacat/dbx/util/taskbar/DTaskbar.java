package de.julianweinelt.datacat.dbx.util.taskbar;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;

@Slf4j
public class DTaskbar {
    private final Frame parent;

    private Taskbar taskbar;

    public DTaskbar(Frame parent) {
        this.parent = parent;
    }

    public boolean isSupported() {
        try {
            taskbar = Taskbar.getTaskbar();
            return true;
        } catch (UnsupportedOperationException e) {
            log.warn(e.getMessage());
            return false;
        }
    }
    public boolean featureSupported(Taskbar.Feature feature) {
        if (taskbar == null) return false;
        return taskbar.isSupported(feature);
    }
    public Taskbar taskbar() {
        return taskbar;
    }
    public DTaskbar progressState(Taskbar.State state) {
        if (!featureSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW)) {
            log.warn("Window progress state could not be updated.");
            return this;
        }
        taskbar.setWindowProgressState(parent, state);
        return this;
    }
    public DTaskbar progressValue(int value, int max) {
        int fullValue = value * 100 / max;
        if (!featureSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW)) {
            log.warn("Window progress value could not be updated.");
            return this;
        }
        taskbar.setWindowProgressValue(parent, fullValue);
        return this;
    }
}