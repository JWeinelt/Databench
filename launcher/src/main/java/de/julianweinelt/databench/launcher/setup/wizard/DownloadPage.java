package de.julianweinelt.databench.launcher.setup.wizard;

import de.julianweinelt.databench.launcher.setup.SetupState;
import de.julianweinelt.databench.launcher.setup.WizardPage;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class DownloadPage implements WizardPage {

    private final SetupState state;
    private JPanel panel;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private boolean downloadsFinished = false;

    public DownloadPage(SetupState state) {
        this.state = state;
        initUI();
    }

    private void initUI() {
        panel = new JPanel(new BorderLayout(10, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        panel.setBackground(UIManager.getColor("Panel.background"));

        JLabel title = new JLabel("Downloading Resources");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        panel.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(panel.getBackground());

        statusLabel = new JLabel("Waiting to start downloads...");
        center.add(statusLabel);
        center.add(Box.createVerticalStrut(10));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        center.add(progressBar);

        panel.add(center, BorderLayout.CENTER);
    }

    @Override
    public void onEnter() {
        if (!downloadsFinished) {
            startDownloads();
        }
    }

    private void startDownloads() {
        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Set<String> drivers = state.selectedDrivers;
                int total = drivers.size();
                for (int i = 0; i < total; i++) {
                    String driver = drivers.stream().toList().get(i);
                    statusLabel.setText("Downloading " + driver + "...");
                    // Simuliere Download
                    for (int p = 0; p <= 100; p += 5) {
                        Thread.sleep(20); // hier Download-Logik einfügen
                        publish(p);
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int last = chunks.get(chunks.size() - 1);
                progressBar.setValue(last);
            }

            @Override
            protected void done() {
                downloadsFinished = true;
                statusLabel.setText("All downloads completed!");
                progressBar.setValue(100);
            }
        };
        worker.execute();
    }

    @Override
    public String getId() {
        return "download";
    }

    @Override
    public JComponent getView() {
        return panel;
    }

    @Override
    public boolean canGoNext() {
        return downloadsFinished;
    }
}
