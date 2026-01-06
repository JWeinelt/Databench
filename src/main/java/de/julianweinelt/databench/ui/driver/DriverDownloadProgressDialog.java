package de.julianweinelt.databench.ui.driver;

import javax.swing.*;
import java.awt.*;

public class DriverDownloadProgressDialog extends JDialog {

    private final JProgressBar progressBar;
    private final JLabel statusLabel;

    public DriverDownloadProgressDialog(Window parent, String db, String version) {
        super(parent, "Downloading Driver", ModalityType.APPLICATION_MODAL);
        setSize(420, 120);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        statusLabel = new JLabel("Preparing download for " + db + " " + version + "...");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        add(statusLabel, BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.setEnabled(false);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(closeButton);
        add(bottom, BorderLayout.SOUTH);

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i <= 100; i += 5) {
                    Thread.sleep(80);
                    publish(i);
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
                statusLabel.setText("Download completed.");
                JOptionPane.showMessageDialog(DriverDownloadProgressDialog.this, "Download completed.", "Info", JOptionPane.INFORMATION_MESSAGE);

                closeButton.setEnabled(true);
                closeButton.doClick();
                parent.dispose();
            }
        };

        worker.execute();
        closeButton.addActionListener(e -> dispose());
    }
}
