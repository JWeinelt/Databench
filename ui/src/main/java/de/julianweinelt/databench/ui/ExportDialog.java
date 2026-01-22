package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.data.ProjectManager;
import de.julianweinelt.databench.dbx.api.ui.dialogs.SelectionDialog;
import de.julianweinelt.databench.dbx.backup.DatabaseExporter;
import de.julianweinelt.databench.dbx.backup.DbxArchiveWriter;
import de.julianweinelt.databench.dbx.backup.ExportListener;
import de.julianweinelt.databench.dbx.database.ADatabase;
import de.julianweinelt.databench.dbx.util.HomeDirectories;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.List;

import static de.julianweinelt.databench.dbx.util.LanguageManager.translate;

@Slf4j
public class ExportDialog extends JDialog {

    private final JProgressBar progressBar = new JProgressBar();
    private final JTextArea console = new JTextArea();
    private final JButton startButton = new JButton(translate("dialog.export.button.start"));
    private final JButton closeButton = new JButton(translate("dialog.export.button.cancel"));
    private final Taskbar taskbar;
    private final Frame parent;

    private List<String> databasesToExport = new ArrayList<>();
    private DbxArchiveWriter writer;
    private ADatabase database;

    public ExportDialog(Frame owner) {
        super(owner, translate("dialog.export.title"), true);
        parent = owner;
        taskbar = Taskbar.getTaskbar();

        setSize(700, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        setModal(false);

        if (taskbar.isSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW)) {
            taskbar.setWindowProgressState(owner, Taskbar.State.NORMAL);
        }

        add(createSettingsPanel(), BorderLayout.NORTH);
        add(createConsolePanel(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        startButton.setEnabled(false);
        startButton.addActionListener(e -> {
            if (!isExportReady()) {
                JOptionPane.showMessageDialog(
                        this,
                        translate("dialog.export.error.not_ready"),
                        translate("dialog.export.error.title"),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            startExport();
        });

        closeButton.addActionListener(e -> dispose());
    }

    private boolean isExportReady() {
        return writer != null && database != null && !databasesToExport.isEmpty();
    }

    private void startExport() {
        startButton.setEnabled(false);
        console.setText("");
        progressBar.setValue(0);
        progressBar.setString(translate("dialog.export.progress.start"));

        ExportListener listener = createListener();

        new Thread(() -> {
            try {
                DatabaseExporter exporter = new DatabaseExporter(writer, database, listener, this);
                exporter.setDatabasesToExport(databasesToExport);
                exporter.retrieveBasicData();
                exporter.createManifest();
                exporter.exportData();

                listener.onLog(translate("dialog.export.console.success"));
                taskbar.setWindowProgressState(this, Taskbar.State.OFF);
            } catch (Exception e) {
                listener.onError(translate("dialog.export.console.failed"), e);
                log.error("Export failed", e);
            } finally {
                try {
                    if (writer != null) writer.close();
                } catch (IOException e) {
                    log.error("Failed to close writer", e);
                }
                SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
            }
        }, "dbx-export-thread").start();
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(translate("dialog.export.settings.title")));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel(translate("dialog.export.label.mode")), gbc);

        JComboBox<String> modeBox = new JComboBox<>(new String[]{
                translate("dialog.export.mode.dbx"),
                translate("dialog.export.mode.mysql.single"),
                translate("dialog.export.mode.mysql.folder")
        });

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(modeBox, gbc);

        JTextArea modeInfo = new JTextArea(4, 20);
        modeInfo.setEditable(false);
        modeInfo.setLineWrap(true);
        modeInfo.setWrapStyleWord(true);
        modeInfo.setBackground(UIManager.getColor("Label.background"));
        modeInfo.setBorder(BorderFactory.createEtchedBorder());

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 3;
        gbc.weightx = 0.6;
        panel.add(modeInfo, gbc);

        modeBox.addActionListener(e ->
                modeInfo.setText(getModeDescription(modeBox.getSelectedIndex()))
        );
        modeBox.setSelectedIndex(0);

        gbc.gridheight = 1;
        gbc.weightx = 0;

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel(translate("dialog.export.label.project")), gbc);

        JComboBox<String> projectBox = new JComboBox<>();
        ProjectManager.instance().getProjects()
                .forEach(p -> projectBox.addItem(p.getName()));

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(projectBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel(translate("dialog.export.label.databases")), gbc);

        JButton selectDbButton = new JButton(translate("dialog.export.button.select"));
        JLabel selectedDbLabel = new JLabel(translate("dialog.export.label.none_selected"));

        JPanel dbPanel = new JPanel(new BorderLayout(6, 0));
        dbPanel.add(selectDbButton, BorderLayout.WEST);
        dbPanel.add(selectedDbLabel, BorderLayout.CENTER);

        gbc.gridx = 1;
        panel.add(dbPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel(translate("dialog.export.label.path")), gbc);

        JTextField pathField = new JTextField();
        JButton browseButton = new JButton(translate("dialog.export.button.browse"));

        JPanel pathPanel = new JPanel(new BorderLayout(6, 0));
        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);

        gbc.gridx = 1;
        panel.add(pathPanel, gbc);

        projectBox.addActionListener(e -> loadDatabases(projectBox, selectedDbLabel));
        selectDbButton.addActionListener(e -> openDatabaseDialog(selectedDbLabel));
        browseButton.addActionListener(e -> chooseExportPath(pathField));

        return panel;
    }

    private void chooseExportPath(JTextField pathField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(translate("dialog.export.choose.title"));
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("DBX (*.dbx)", "dbx"));

        if (!pathField.getText().isBlank()) {
            chooser.setSelectedFile(new File(pathField.getText()));
        } else {
            chooser.setCurrentDirectory(HomeDirectories.instance().get("Export"));
        }

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".dbx")) {
            file = new File(file.getAbsolutePath() + ".dbx");
        }

        if (file.exists()) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    translate("dialog.export.confirm.overwrite"),
                    translate("dialog.export.confirm.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (result != JOptionPane.YES_OPTION) return;
        }

        HomeDirectories.instance().put("Export", file.getParent());
        pathField.setText(file.getAbsolutePath());

        try {
            writer = new DbxArchiveWriter(file.toPath());
            updateStartButton();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    translate("dialog.export.error.init"),
                    JOptionPane.ERROR_MESSAGE
            );
            log.error("Failed to create writer", e);
        }
    }

    private JScrollPane createConsolePanel() {
        console.setEditable(false);
        console.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane pane = new JScrollPane(console);
        pane.setBorder(BorderFactory.createTitledBorder(translate("dialog.export.console.title")));
        return pane;
    }

    private JPanel createFooter() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        progressBar.setStringPainted(true);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(startButton);
        buttons.add(closeButton);

        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private void updateStartButton() {
        startButton.setEnabled(isExportReady());
    }

    private String getModeDescription(int index) {
        return switch (index) {
            case 0 -> translate("dialog.export.mode.desc.dbx");
            case 1 -> translate("dialog.export.mode.desc.mysql.single");
            case 2 -> translate("dialog.export.mode.desc.mysql.folder");
            default -> "";
        };
    }

    private void loadDatabases(JComboBox<String> projectBox, JLabel label) {
        Project project = ProjectManager.instance().getProject(
                (String) projectBox.getSelectedItem()
        );
        if (project == null) return;

        database = ADatabase.of(
                project.getDatabaseType(),
                project.getServer().split(":")[0],
                project.getServer().contains(":")
                        ? Integer.parseInt(project.getServer().split(":")[1])
                        : 3306,
                project.getUsername(),
                project.getPassword()
        );

        if (database.connect()) {
            openDatabaseDialog(label);
        }
    }

    private void openDatabaseDialog(JLabel label) {
        SelectionDialog dialog = new SelectionDialog(
                parent,
                database.getDatabases().stream()
                        .filter(db -> !db.endsWith("_schema"))
                        .toList()
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            databasesToExport = dialog.getSelectedNames();
            label.setText(String.join(", ", databasesToExport));
            updateStartButton();
        }
    }

    private ExportListener createListener() {
        return new ExportListener() {

            @Override
            public void onProgress(int current, int total, String message) {
                int percent = total == 0 ? 0 : (int) (current * 100.0 / total);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setMaximum(total);
                    progressBar.setValue(current);
                    progressBar.setString(message + " (" + current + "/" + total + ")");
                    if (taskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE)) {
                        taskbar.setWindowProgressValue(ExportDialog.this, percent);
                    }
                });
            }

            @Override
            public void onLog(String message) {
                SwingUtilities.invokeLater(() -> {
                    console.append(message + "\n");
                    console.setCaretPosition(console.getDocument().getLength());
                });
            }

            @Override
            public void onError(String message, Throwable throwable) {
                SwingUtilities.invokeLater(() -> {
                    console.append("[ERROR] " + message + "\n");
                    console.append(throwable.getMessage() + "\n");
                });
            }

            @Override
            public void save() {
                try {
                    String name = "export_log_" + Instant.now().toEpochMilli() + ".txt";
                    try (FileWriter fw = new FileWriter(new File(name))) {
                        fw.write(console.getText());
                    }
                } catch (IOException e) {
                    onError(translate("dialog.export.error.save_log"), e);
                }
            }
        };
    }
}