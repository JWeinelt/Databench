package de.julianweinelt.databench.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class FileCopyProgressDialog extends JDialog {

    private final JProgressBar progressBar;
    private final JLabel statusLabel;

    public FileCopyProgressDialog(Window parent, File source, File target, Runnable onSuccess) {
        super(parent, "Updating...", ModalityType.APPLICATION_MODAL);

        setSize(420, 120);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        statusLabel = new JLabel("Preparing copy...");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        add(statusLabel, BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (target.getParentFile() != null) target.getParentFile().mkdirs();

                long totalBytes = source.length();
                long copied = 0;

                try (InputStream in = new FileInputStream(source);
                     OutputStream out = new FileOutputStream(target)) {

                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        copied += read;
                        int progress = (int) (copied * 100 / totalBytes);
                        publish(progress);
                    }
                }

                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int value = chunks.get(chunks.size() - 1);
                progressBar.setValue(value);
                statusLabel.setText("Copying... " + value + "%");
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Copy complete!");
                    onSuccess.run();
                    dispose();
                } catch (Exception e) {
                    statusLabel.setText("Copy failed: " + e.getMessage());
                    dispose();
                }
            }
        };

        worker.execute();
    }
}