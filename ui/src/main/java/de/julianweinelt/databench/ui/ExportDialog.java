package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.data.ProjectManager;
import de.julianweinelt.databench.dbx.api.ui.dialogs.SelectionDialog;
import de.julianweinelt.databench.dbx.backup.DatabaseExporter;
import de.julianweinelt.databench.dbx.backup.DbxArchiveWriter;
import de.julianweinelt.databench.dbx.backup.ExportListener;
import de.julianweinelt.databench.dbx.database.ADatabase;
import de.julianweinelt.databench.dbx.database.DBMySQL;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
public class ExportDialog extends JDialog {
    private final JProgressBar progressBar = new JProgressBar();
    private final JTextArea console = new JTextArea();
    private final JButton startButton = new JButton("Start Export");
    private final JButton closeButton = new JButton("Close");
    private final Taskbar taskbar;
    private List<String> databasesToExport = new ArrayList<>();

    private DbxArchiveWriter writer = null;
    private final Frame parent;
    private ADatabase database = null;


    public ExportDialog(Frame owner) {
        super(owner, "DataBench Export", true);
        taskbar = Taskbar.getTaskbar();
        parent = owner;
        if (taskbar.isSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW))
            taskbar.setWindowProgressState(owner, Taskbar.State.NORMAL);
        else taskbar.setWindowProgressState(owner, Taskbar.State.INDETERMINATE);
        setSize(700, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        setModal(false);
        setAlwaysOnTop(false);

        add(createSettingsPanel(), BorderLayout.NORTH);
        add(createConsolePanel(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        startButton.setEnabled(false);
        startButton.addActionListener(e -> {
            if (!validateExportState()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please select a project, at least one database and an export path.",
                        "Export not ready",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            startExport(writer, database);
        });
    }

    private boolean validateExportState() {
        return writer != null
                && database != null
                && databasesToExport != null
                && !databasesToExport.isEmpty();
    }

    private void startExport(
            DbxArchiveWriter writer,
            ADatabase database
    ) {
        startButton.setEnabled(false);
        console.setText("");
        progressBar.setValue(0);
        progressBar.setString("Starting export...");

        ExportListener listener = createListener();

        if (taskbar.isSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW)) {
            taskbar.setWindowProgressState(this, Taskbar.State.NORMAL);
        }

        new Thread(() -> {
            try {
                DatabaseExporter exporter = new DatabaseExporter(
                        writer,
                        database,
                        listener,
                        this
                );
                exporter.setDatabasesToExport(databasesToExport);

                exporter.retrieveBasicData();
                exporter.createManifest();
                exporter.exportData();

                listener.onLog("Export completed successfully.");
                taskbar.setWindowProgressState(this, Taskbar.State.OFF);

            } catch (Exception e) {
                listener.onError("Export failed", e);
                log.error("Export failed", e);
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.error("Failed to close archive writer", e);
                }

                SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
            }
        }, "dbx-export-thread").start();
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Export Settings"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Export mode:"), gbc);

        JComboBox<String> modeBox = new JComboBox<>(new String[]{
                "DBX Archive",
                "MySQL Dump (single file)",
                "MySQL Dump (folder)"
        });

        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(modeBox, gbc);

        JTextArea modeInfo = new JTextArea(4, 20);
        modeInfo.setEditable(false);
        modeInfo.setLineWrap(true);
        modeInfo.setWrapStyleWord(true);
        modeInfo.setBackground(UIManager.getColor("Label.background"));
        modeInfo.setBorder(BorderFactory.createEtchedBorder());

        gbc.gridx = 2; gbc.gridy = 0;
        gbc.gridheight = 3;
        gbc.weightx = 0.6;
        panel.add(modeInfo, gbc);

        modeBox.addActionListener(e ->
                modeInfo.setText(getModeDescription((String) modeBox.getSelectedItem()))
        );
        modeBox.setSelectedIndex(0);

        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Project:"), gbc);

        JComboBox<String> projectBox = new JComboBox<>();
        ProjectManager.instance().getProjects()
                .forEach(p -> projectBox.addItem(p.getName()));

        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(projectBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JLabel("Databases:"), gbc);

        JButton selectDbButton = new JButton("Select…");
        JLabel selectedDbLabel = new JLabel("No databases selected");

        JPanel dbPanel = new JPanel(new BorderLayout(6, 0));
        dbPanel.add(selectDbButton, BorderLayout.WEST);
        dbPanel.add(selectedDbLabel, BorderLayout.CENTER);

        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(dbPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panel.add(new JLabel("Export path:"), gbc);

        JTextField pathField = new JTextField();
        JButton browseButton = new JButton("...");

        JPanel pathPanel = new JPanel(new BorderLayout(6, 0));
        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);

        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(pathPanel, gbc);

        projectBox.addActionListener(e -> loadDatabases(projectBox, selectedDbLabel));
        selectDbButton.addActionListener(e -> openDatabaseDialog(selectedDbLabel));

        browseButton.addActionListener(e -> chooseExportPath(pathField));

        return panel;
    }

    private void chooseExportPath(JTextField pathField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose export file");
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("DataBench export (*.dbx)", "dbx"));

        if (!pathField.getText().isBlank()) {
            chooser.setSelectedFile(new File(pathField.getText()));
        } else {
            chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        }

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();

        if (!selectedFile.getName().toLowerCase().endsWith(".dbx")) {
            selectedFile = new File(selectedFile.getAbsolutePath() + ".dbx");
            updateStartButtonState();
        }

        if (selectedFile.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(
                    this,
                    "The file already exists.\nDo you want to overwrite it?",
                    "Confirm overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
            updateStartButtonState();
        }

        pathField.setText(selectedFile.getAbsolutePath());

        try {
            writer = new DbxArchiveWriter(selectedFile.toPath());
            startButton.setEnabled(true);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Export initialization failed",
                    JOptionPane.ERROR_MESSAGE
            );
            log.error("Failed to create DbxArchiveWriter", ex);
        }
    }

    private JScrollPane createConsolePanel() {
        console.setEditable(false);
        console.setFont(new Font("Consolas", Font.PLAIN, 12));
        console.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(console);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Export Log"));

        return scrollPane;
    }

    private JPanel createFooter() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        progressBar.setStringPainted(true);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(startButton);
        buttons.add(closeButton);

        closeButton.addActionListener(e -> dispose());

        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);

        return panel;
    }

    private void updateStartButtonState() {
        startButton.setEnabled(
                writer != null &&
                        database != null &&
                        databasesToExport != null &&
                        !databasesToExport.isEmpty()
        );
    }

    private String getModeDescription(String mode) {
        return switch (mode) {
            case "DBX Archive" ->
                    "Creates a .dbx archive containing schema, data and metadata.\nRecommended for backups and migration.";
            case "MySQL Dump (single file)" ->
                    "Exports all selected databases into one SQL dump file.\nThis method may lead to problems when importing!";
            case "MySQL Dump (folder)" ->
                    "Creates one SQL file per database in a target directory.\nThis method may lead to problems when importing!";
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
                Integer.parseInt(project.getServer().contains(":")
                        ? project.getServer().split(":")[1] : "3306"),
                project.getUsername(),
                project.getPassword(),
                project.getDefaultDatabase()
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
            updateStartButtonState();
        }
    }

    private ExportListener createListener() {
        return new ExportListener() {

            @Override
            public void onProgress(int current, int total, String message) {
                int pr = Math.min(100, Math.max(0, (int) (current * 100.0 / total)));
                SwingUtilities.invokeLater(() -> {
                    progressBar.setMaximum(total);
                    progressBar.setValue(current);
                    progressBar.setString(message + " (" + current + "/" + total + ")");
                    if (taskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE))
                        taskbar.setWindowProgressValue(ExportDialog.this, pr);
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
                    console.append("Error thrown: " + throwable.getMessage() + "\n");
                    for (StackTraceElement element : throwable.getStackTrace()) {
                        console.append(element.toString() + "\n");
                    }
                });
            }

            @Override
            public void save() {
                String text = console.getText();
                Date date = Date.from(Instant.now());
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                String dateName = c.get(Calendar.HOUR) + "_" + c.get(Calendar.MINUTE) + "_" + c.get(Calendar.SECOND)
                        + "_" + c.get(Calendar.DAY_OF_MONTH) + "_" + (c.get(Calendar.MONTH) + 1)
                        + "_" + c.get(Calendar.YEAR);
                File saveFile = new File("export_log_" + dateName + ".txt");
                try (FileWriter fw = new FileWriter(saveFile)) {
                    fw.write(text);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    onError("Error saving log file", e);
                }
            }
        };
    }
}
