package de.julianweinelt.databench.ui.editor;

@FunctionalInterface
public interface ProgressCallback {
    void update(int currentRow);
}
