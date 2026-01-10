package de.julianweinelt.databench.launcher;

import de.julianweinelt.databench.launcher.storage.LocalStorage;
import de.julianweinelt.databench.launcher.ui.FileCopyProgressDialog;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Slf4j
public class Launcher {
    private final File appFile = new File("DataBench/DataBench.jar");
    private final File libraryFolder = new File("lib");
    private final File mssqlLib = new File(libraryFolder, "mssql-jdbc-auth.dll");

    private final LocalStorage storage = new LocalStorage();

    public static void main(String[] args) {
        new Launcher().start(args);
    }

    private void start(String[] args) {
        // If first start
        if (!storage.getConfigFile().exists()) {
            if (libraryFolder.mkdirs()) log.debug("lib folder created successfully");
            if (appFile.getParentFile().mkdirs()) log.debug("DataBench folder created successfully");
            try {
                File tmp1 = new File("DataBench.jar");
                File tmp2 = new File("mssql-jdbc-auth.dll");
                Files.copy(tmp1.toPath(), appFile.toPath());
                if (tmp1.delete()) log.debug("DataBench.jar deleted successfully");
                Files.copy(tmp2.toPath(), mssqlLib.toPath());
                if (tmp2.delete()) log.debug("mssql-jdbc-auth.dll deleted successfully");
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            storage.save();
        }

        storage.load();

        if (args.length != 0) {
            List<String> a = List.of(args);
            if (a.contains("--update")) {
                update();
                return;
            }
        }

        startJar(args);
    }

    private void startJar(String[] mainArgs) {
        String[] args = new String[6 + storage.getConfiguration().getJvmArgs().size() + mainArgs.length];
        args[0] = "java";
        args[1] = "-Djava.library.path=" + mssqlLib.getAbsolutePath();
        args[2] = "-Xmx" + storage.getConfiguration().getMaxMemMB() + "M";
        args[3] = "-Xms" + storage.getConfiguration().getMaxMemMB() + "M";
        int i = 4;
        for (String arg : storage.getConfiguration().getJvmArgs()) {
            args[i++] = arg;
        }
        args[i++] = "-jar";
        args[i++] = appFile.getAbsolutePath();
        for (String arg : mainArgs) {
            args[i++] = arg;
        }

        ProcessBuilder pb = new ProcessBuilder(args);
        try {
            pb.start();
        } catch (IOException e) {
            log.error(e.getMessage());
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

        new FileCopyProgressDialog(null, file, appFile, () -> {
            if (file.delete()) log.debug("Backup deleted successfully");
            String[] args = new String[1];
            args[0] = "--update";
            startJar(args);
        }).setVisible(true);
    }
}