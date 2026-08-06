package de.julianweinelt.datacat.ui;

import de.julianweinelt.datacat.data.Project;
import de.julianweinelt.datacat.data.ProjectManager;
import de.julianweinelt.datacat.dbx.database.ADatabase;
import de.julianweinelt.datacat.dbx.backup.DatabaseImporter;
import de.julianweinelt.datacat.dbx.backup.DbxArchiveReader;
import de.julianweinelt.datacat.dbx.backup.ImportListener;
import de.julianweinelt.datacat.dbx.util.taskbar.DTaskbar;
import de.julianweinelt.datacat.ui.importdiag.DatabaseMappingDialog;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.julianweinelt.datacat.dbx.util.LanguageManager.translate;

@Slf4j
public class ImportDialog extends JDialog implements ImportListener {

    private final JTextField archiveField = new JTextField();
    private final JTextField targetField = new JTextField();
    private final JTextArea logArea = new JTextArea();
    private final JProgressBar progressBar = new JProgressBar();

    private final JButton startButton = new JButton(translate("dialog.import.button.start"));
    private final JButton cancelButton = new JButton(translate("dialog.export.button.cancel"));
    private final JButton browseButton = new JButton("...");

    private final JTextField databasesField = new JTextField();
    private final JComboBox<String> importModeBox = new JComboBox<>(new String[]{
            translate("dialog.import.mode.replace"),
            translate("dialog.import.mode.merge"),
            translate("dialog.import.mode.skip-existing")
    });
    private final JButton databasesButton =
            new JButton(translate("dialog.import.button.databases"));

    private final DTaskbar dTaskbar;

    private Thread importThread;

    private DbxArchiveReader archiveReader;
    private DatabaseImporter importer;

    private ADatabase targetDatabase = null;

    private List<String> databasesLoadedFromServer;

    private final Map<String, String> importDBMappings = new HashMap<>();

    public ImportDialog(Frame owner) {
        super(owner, translate("dialog.import.title"), true);
        BenchUI.addEscapeKeyBind(this);

        dTaskbar = new DTaskbar(owner);
        try {
            if (dTaskbar.featureSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW)) {
                dTaskbar.progressState(Taskbar.State.NORMAL);
            }
        } catch (UnsupportedOperationException e) {
            log.warn("Taskbar features not supported on this system.");
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (dTaskbar.featureSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW)) {
                    dTaskbar.progressState(Taskbar.State.OFF);
                }
            }
        });

        setSize(700, 450);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        initUI();
        wireActions();
        startButton.setEnabled(false);
    }

    private void initUI() {
        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createTitledBorder(
                translate("dialog.import.settings")
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        archiveField.setEditable(false);
        targetField.setEditable(false);
        databasesField.setEditable(false);

        JComboBox<String> projects = new JComboBox<>();
        projects.addItem(translate("dialog.import.select.project"));

        ProjectManager.instance().getProjects()
                .forEach(project -> projects.addItem(project.getName()));

        projects.addActionListener(e -> {
            String selected = (String) projects.getSelectedItem();

            if (selected == null) return;
            if (selected.equals(
                    translate("dialog.import.select.project")
            )) return;

            Project project =
                    ProjectManager.instance().getProject(selected);

            targetDatabase = ADatabase.of(
                    project.getDatabaseType(),
                    project.getServer().split(":")[0],
                    project.getServer().split(":").length == 1
                            ? 3306
                            : Integer.parseInt(
                            project.getServer().split(":")[1]
                    ),
                    project.getUsername(),
                    project.getPassword()
            );
            if (targetDatabase.connect()) {
                databasesLoadedFromServer = targetDatabase.getDatabases();
                makeDBChecks();
            } else {
                log.error("Could not connect to database!");
            }

            targetField.setText(project.getName());
        });

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        top.add(
                new JLabel(translate("dialog.import.project")),
                c
        );

        c.gridx = 1;
        c.weightx = 1;
        top.add(projects, c);

        c.gridx = 2;
        c.weightx = 0;
        top.add(
                new JLabel(translate("dialog.import.mode")),
                c
        );

        c.gridx = 3;
        c.weightx = 1;
        top.add(importModeBox, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        top.add(
                new JLabel(translate("dialog.import.archive")),
                c
        );

        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        top.add(archiveField, c);

        c.gridx = 3;
        c.gridwidth = 1;
        c.weightx = 0;
        top.add(browseButton, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        top.add(
                new JLabel(translate("dialog.import.databases")),
                c
        );

        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        top.add(databasesField, c);

        c.gridx = 3;
        c.gridwidth = 1;
        c.weightx = 0;
        top.add(databasesButton, c);

        add(top, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(
                new Font(Font.MONOSPACED, Font.PLAIN, 12)
        );

        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(progressBar, BorderLayout.CENTER);

        JPanel buttons =
                new JPanel(new FlowLayout(FlowLayout.RIGHT));

        buttons.add(cancelButton);
        buttons.add(startButton);

        bottom.add(buttons, BorderLayout.SOUTH);

        add(bottom, BorderLayout.SOUTH);
    }

    private void wireActions() {
        browseButton.addActionListener(e -> chooseArchive());
        cancelButton.addActionListener(e -> {
            int val = JOptionPane.showConfirmDialog(ImportDialog.this,
                    translate("dialog.import.cancel.dialog.text"),
                    translate("dialog.import.cancel.dialog.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
            if (val == JOptionPane.YES_OPTION) {
                if (importThread != null && importThread.isAlive()) importThread.interrupt();
                try {
                    targetDatabase.rollback();
                } catch (Exception ignored) {}
                dispose();
                dTaskbar.progressState(Taskbar.State.OFF);
            }
        });
        startButton.addActionListener(e -> startImport());
        databasesButton.addActionListener(e -> makeDBChecks());
    }

    private void chooseArchive() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter(translate("dialog.export.extension.dbx"), "dbx"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            archiveField.setText(file.getAbsolutePath());
            createImportReader();
            makeDBChecks();
        }
    }

    private void makeDBChecks() {
        if (archiveReader == null) return;
        if (targetDatabase != null) {
            //startButton.setEnabled(true);
            if (databasesLoadedFromServer != null) {
                openDatabaseSelection();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        translate("dialog.import.connection-failed"),
                        translate("dialog.import.error.title"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    translate("dialog.import.select-project-first"),
                    translate("dialog.import.error.title"),
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void createImportReader() {
        try {
            archiveReader = new DbxArchiveReader(new File(archiveField.getText()).toPath());

            importer = new DatabaseImporter(archiveReader, targetDatabase, this, this);
            importer.readManifest();
        } catch (IOException e) {
            logArea.append(translate("dialog.import.error.init") + "\n");
        }
    }


    private void startImport() {
        if (archiveField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    translate("dialog.import.missing-file.text"),
                    translate("dialog.import.missing-file.title"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        startButton.setEnabled(false);
        browseButton.setEnabled(false);
        logArea.setText("");
        progressBar.setValue(0);

        message("======== DATABASE MAPPINGS ======");
        importDBMappings.forEach((source, target) -> message(source + " -> " + target));
        message("=================================");

        importThread = new Thread(() -> {
            try {
                message(translate("dialog.import.log.readmanifest"));
                importer.readManifest();
                message(translate("dialog.import.log.validate"));
                importer.validate();
                message(translate("dialog.import.log.connecting"));
                importer.connectTarget();
                message(translate("dialog.import.log.loading"));
                importer.loadSchemas();
                message(translate("dialog.import.log.importing"));
                importer.importData();
                dTaskbar.progressState(Taskbar.State.OFF);

            } catch (Exception ex) {
                dTaskbar.progressState(Taskbar.State.ERROR);
                onError("Import failed", ex);
                message(translate("dialog.import.import-failed.text", Map.of("error", ex.getMessage())));
                SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                this,
                                translate("dialog.import.import-failed.text", Map.of("error", ex.getMessage())),
                                translate("dialog.import.import-failed.title"),
                                JOptionPane.ERROR_MESSAGE
                        );
                    dTaskbar.progressState(Taskbar.State.OFF);
                    }
                );
            }
        }, "dbx-import-thread");
        importThread.start();
    }

    private void openDatabaseSelection() {
        DatabaseMappingDialog dialog =
                new DatabaseMappingDialog(
                        this,
                        importer.getDatabases(),
                        databasesLoadedFromServer
                );

        dialog.setVisible(true);

        if (dialog.isConfirmed()) {

            Map<String, String> mappings =
                    dialog.getMappings();

            importDBMappings.clear();
            importDBMappings.putAll(mappings);
            startButton.setEnabled(true);
        }
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onProgress(int current, int total) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setMaximum(total);
            progressBar.setValue(current);
            progressBar.setStringPainted(true);
            if (dTaskbar.featureSupported(Taskbar.Feature.PROGRESS_VALUE))
                dTaskbar.progressValue(current, total);
        });
    }

    @Override
    public void message(String message) {
        progressBar.setString(message);
    }

    @Override
    public void onError(String message, Throwable e) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(translate("dialog.import.log.error.prefix") + " " + message + "\n");
            if (e != null) {
                logArea.append("  " + e.getMessage() + "\n");
            }
        });
    }
}
