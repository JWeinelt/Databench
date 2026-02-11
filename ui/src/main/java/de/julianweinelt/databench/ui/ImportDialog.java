package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.data.ProjectManager;
import de.julianweinelt.databench.dbx.database.ADatabase;
import de.julianweinelt.databench.dbx.backup.DatabaseImporter;
import de.julianweinelt.databench.dbx.backup.DbxArchiveReader;
import de.julianweinelt.databench.dbx.backup.ImportListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.Map;

import static de.julianweinelt.databench.dbx.util.LanguageManager.translate;

public class ImportDialog extends JDialog implements ImportListener {

    private final JTextField archiveField = new JTextField();
    private final JTextField targetField = new JTextField();
    private final JTextArea logArea = new JTextArea();
    private final JProgressBar progressBar = new JProgressBar();

    private final JButton startButton = new JButton(translate("dialog.import.button.start"));
    private final JButton cancelButton = new JButton(translate("dialog.export.button.cancel"));
    private final JButton browseButton = new JButton("...");
    private final Taskbar taskbar;

    private Thread importThread;

    private ADatabase targetDatabase = null;

    public ImportDialog(Frame owner) {
        super(owner, translate("dialog.import.title"), true);
        BenchUI.addEscapeKeyBind(this);

        taskbar = Taskbar.getTaskbar();
        if (!taskbar.isSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW)) {
            taskbar.setWindowProgressState(owner, Taskbar.State.INDETERMINATE);
        } else {
            taskbar.setWindowProgressState(owner, Taskbar.State.NORMAL);
        }

        setSize(700, 450);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        initUI();
        wireActions();
        startButton.setEnabled(false);
    }

    private void initUI() {
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        archiveField.setEditable(false);
        targetField.setEditable(false);
        //targetField.setText(targetDatabase);

        c.gridx = 0; c.gridy = 0;
        JComboBox<String> projects = new JComboBox<>();
        projects.addItem(translate("dialog.import.select.project"));
        ProjectManager.instance().getProjects().forEach(project -> projects.addItem(project.getName()));
        projects.addActionListener(e -> {
            String selected = (String) projects.getSelectedItem();
            if (selected == null) return;
            if (selected.equals(translate("dialog.import.select.project"))) return;

            Project project = ProjectManager.instance().getProject(selected);

            targetDatabase = ADatabase.of(
                    project.getDatabaseType(),
                    project.getServer().split(":")[0],
                    (project.getServer().split(":").length == 1) ? 3306 : Integer.parseInt(project.getServer().split(":")[1]),
                    project.getUsername(),
                    project.getPassword()
            );
            if (!archiveField.getText().isEmpty())
                startButton.setEnabled(true);
            targetField.setText(project.getName());

        });
        top.add(projects, c);

        c.gridx = 0; c.gridy = 1;
        top.add(new JLabel(translate("dialog.import.archive")), c);

        c.gridx = 1; c.weightx = 1;
        top.add(archiveField, c);

        c.gridx = 2; c.weightx = 0;
        top.add(browseButton, c);

        c.gridx = 0; c.gridy = 2;
        top.add(new JLabel(translate("dialog.import.target-db")), c);

        c.gridx = 1; c.gridwidth = 2;
        top.add(targetField, c);

        add(top, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(progressBar, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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
                targetDatabase.rollback();
                dispose();
            }
        });
        startButton.addActionListener(e -> startImport());
    }

    private void chooseArchive() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter(translate("dialog.export.extension.dbx"), "dbx"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            archiveField.setText(file.getAbsolutePath());
            if (targetDatabase != null) {
                startButton.setEnabled(true);
            }
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

        importThread = new Thread(() -> {
            try {
                DbxArchiveReader reader =
                        new DbxArchiveReader(new File(archiveField.getText()).toPath());

                DatabaseImporter importer =
                        new DatabaseImporter(reader, targetDatabase, this, this);

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
                taskbar.setWindowProgressState(this, Taskbar.State.OFF);

            } catch (Exception ex) {
                taskbar.setWindowProgressState(this, Taskbar.State.ERROR);
                onError("Import failed", ex);
                message(translate("dialog.import.import-failed.text", Map.of("error", ex.getMessage())));
                SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                this,
                                translate("dialog.import.import-failed.text", Map.of("error", ex.getMessage())),
                                translate("dialog.import.import-failed.title"),
                                JOptionPane.ERROR_MESSAGE
                        );
                    taskbar.setWindowProgressState(this, Taskbar.State.OFF);
                    }
                );
            }
        }, "dbx-import-thread");
        importThread.start();
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
            if (taskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE))
                taskbar.setWindowProgressValue(this, current * 100 / total);
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
