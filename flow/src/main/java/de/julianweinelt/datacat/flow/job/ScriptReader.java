package de.julianweinelt.datacat.flow.job;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScriptReader {
    public void parseScript(Path scriptPath) {
        try {
            String content = Files.readString(scriptPath);

        } catch (IOException e) {

        }
    }
}
