package de.julianweinelt.databench.api;

import java.io.File;

public interface IFileWatcherListener {
    void fileTreeUpdate(FileTree tree);
}
