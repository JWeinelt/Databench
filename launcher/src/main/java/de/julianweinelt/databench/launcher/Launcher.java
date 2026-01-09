package de.julianweinelt.databench.launcher;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Slf4j
public class Launcher {
    private final File appFile = new File("../DataBench/DataBench.jar");

    public static void main(String[] args) {
    }

    private void start(String[] args) {
        if (args.length != 0) {
            List<String> a = List.of(args);
            if (a.contains("--update")) {

            }
        }
    }

    private void update() {
        File file = new File("tmp/dbench-update.jar");
        try {
            Files.copy(appFile.toPath(), new File("tmp/editor-backup.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error(e.getMessage());
            JOptionPane.showMessageDialog(null, "Error while creating backup of existing installation:\n" + e.getMessage(),
                    "Update Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Files.copy(file.toPath(), appFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}