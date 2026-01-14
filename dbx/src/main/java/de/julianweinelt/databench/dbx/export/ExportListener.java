package de.julianweinelt.databench.dbx.export;

public interface ExportListener {

    /** 
     * Called, when the progress changes
     * @param current current step
     * @param total total step amount
     * @param message optional description
     */
    void onProgress(int current, int total, String message);

    /**
     * Log output for UI console
     */
    void onLog(String message);

    void onError(String message, Throwable throwable);
}
