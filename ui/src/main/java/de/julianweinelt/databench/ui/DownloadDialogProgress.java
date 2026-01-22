package de.julianweinelt.databench.ui;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static de.julianweinelt.databench.dbx.util.LanguageManager.translate;

@Slf4j
public class DownloadDialogProgress extends JDialog {

    private final JProgressBar progressBar;
    private final JLabel statusLabel;

    public DownloadDialogProgress(Window parent, String fileUrl, File saveFile, String translationCategory, Runnable onSuccess) {
        super(parent, translate("dialog." + translationCategory + ".download.progress.title"), ModalityType.APPLICATION_MODAL);
        if (saveFile.getParentFile().mkdirs()) log.debug("Created directory {}", saveFile.getParentFile().getAbsolutePath());
        setSize(420, 120);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        statusLabel = new JLabel(translate("dialog." + translationCategory + ".download.progress.prepare"));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        add(statusLabel, BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);

        JButton closeButton = new JButton(translate("dialog." + translationCategory + ".download.button.close"));
        closeButton.setEnabled(false);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(closeButton);
        add(bottom, BorderLayout.SOUTH);

        closeButton.addActionListener(e -> dispose());

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                statusLabel.setText(translate("dialog." + translationCategory + ".download.progress.description"));
                log.info("Downloading driver from {}", fileUrl);
                log.info("Saving to {}", saveFile.getAbsolutePath());
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36");
                int contentLength = connection.getContentLength();

                if (contentLength < 0) {
                    publish(0);
                }

                try (InputStream in = connection.getInputStream();
                     OutputStream out = new FileOutputStream(saveFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalRead = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                        if (contentLength > 0) {
                            int progress = (int) (totalRead * 100 / contentLength);
                            publish(progress);
                        }
                    }
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                int value = chunks.get(chunks.size() - 1);
                progressBar.setValue(value);
                statusLabel.setText("Downloading... " + value + "%");
            }

            @Override
            protected void done() {
                try {
                    get();

                    onSuccess.run();

                    JOptionPane.showMessageDialog(DownloadDialogProgress.this,
                            translate("dialog." + translationCategory + ".download.success.description"),
                            translate("dialog." + translationCategory + ".download.success.title"),
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception e) {
                    statusLabel.setText("Download failed.");
                    JOptionPane.showMessageDialog(DownloadDialogProgress.this,
                            translate("dialog." + translationCategory + ".download.error.description", Map.of(
                                    "error", e.getMessage()
                            )),
                            translate("dialog." + translationCategory + ".download.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                    dispose();
                }
                closeButton.setEnabled(true);
            }
        };

        worker.execute();
    }
}