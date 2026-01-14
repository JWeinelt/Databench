package de.julianweinelt.databench.ui.driver;

import de.julianweinelt.databench.dbx.api.drivers.DriverManagerService;
import de.julianweinelt.databench.util.ArchiveUtils;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.DriverManager;
import java.util.Map;

import static de.julianweinelt.databench.ui.LanguageManager.translate;

@Slf4j
public class DriverDownloadProgressDialog extends JDialog {

    private final JProgressBar progressBar;
    private final JLabel statusLabel;

    public DriverDownloadProgressDialog(Window parent, String fileUrl, File saveFolder, DriverDownloadWrapper.DriverDownload download) {
        super(parent, translate("dialog.driver.download.progress.title"), ModalityType.APPLICATION_MODAL);
        File saveFile = new File(saveFolder, download.fileName());
        saveFolder.mkdirs();
        setSize(420, 120);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        statusLabel = new JLabel(translate("dialog.driver.download.progress.prepare"));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        add(statusLabel, BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);

        JButton closeButton = new JButton(translate("dialog.driver.download.button.close"));
        closeButton.setEnabled(false);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(closeButton);
        add(bottom, BorderLayout.SOUTH);

        closeButton.addActionListener(e -> dispose());

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                statusLabel.setText(translate("dialog.driver.download.progress.description", Map.of(
                        "dbtype", "MySQL", //TODO: Make dynamic
                        "version", "unknown" // TODO: Make dynamic
                )));
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
                    if (download.zipped()) {
                        statusLabel.setText(translate("dialog.driver.download.progress.extract"));
                        if (saveFile.getName().endsWith(".zip")) {
                            ArchiveUtils.unzip(saveFile, saveFolder);
                        }
                        if (saveFile.getName().endsWith(".tar.gz")) {
                            ArchiveUtils.untarGz(saveFile, new File(saveFolder, "tmp"));
                        }
                    }

                    statusLabel.setText(translate("dialog.driver.download.progress.install"));
                    DriverDownloadWrapper.postProcess(saveFile);

                    DriverManagerService.instance().preloadDrivers();
                    JOptionPane.showMessageDialog(DriverDownloadProgressDialog.this,
                            translate("dialog.driver.download.success.description", Map.of(
                                    "dbtype", saveFile.getName() //TODO: Use DB Type
                            )),
                            translate("dialog.driver.download.success.title"),
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    statusLabel.setText("Download failed.");
                    JOptionPane.showMessageDialog(DriverDownloadProgressDialog.this,
                            translate("dialog.driver.download.error.description", Map.of(
                                    "dbtype", "MySQL", //TODO: Use DB Type
                                    "version", "unknown", // TODO: Make dynamic
                                    "error", e.getMessage()
                            )),
                            translate("dialog.driver.download.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                }
                closeButton.setEnabled(true);
            }
        };

        worker.execute();
    }
}