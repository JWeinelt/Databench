package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.data.ProjectManager;
import de.julianweinelt.databench.dbx.database.ADatabase;
import de.julianweinelt.databench.dbx.database.DBMySQL;
import de.julianweinelt.databench.dbx.export.DatabaseExporter;
import de.julianweinelt.databench.dbx.export.DbxArchiveWriter;
import de.julianweinelt.databench.dbx.export.ExportListener;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

@Slf4j
public class ExportDialog extends JDialog {
    private final JProgressBar progressBar = new JProgressBar();
    private final JTextArea console = new JTextArea();
    private final JButton startButton = new JButton("Start Export");
    private final JButton closeButton = new JButton("Close");

    private DbxArchiveWriter writer = null;
    private ADatabase database = null;


    public ExportDialog(Frame owner) {
        super(owner, "DataBench Export", true);
        setSize(700, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        setModal(false);
        setAlwaysOnTop(false);

        add(createSettingsPanel(), BorderLayout.NORTH);
        add(createConsolePanel(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Export Settings"));

        JComboBox<String> projectComboBox = new JComboBox<>();
        projectComboBox.setModel(new DefaultComboBoxModel<>(new String[] {}));
        projectComboBox.addItem("Select Project");
        ProjectManager.instance().getProjects().forEach(project -> projectComboBox.addItem(project.getName()));
        projectComboBox.addActionListener(e -> {
            if (projectComboBox.getSelectedItem() == null) return;
            String selectedProject = (String) projectComboBox.getSelectedItem();
            if (selectedProject.equals("Select Project")) return;
            log.info("Selected Project: {}", selectedProject);
            Project project = ProjectManager.instance().getProject(selectedProject);
            if (project == null) {
                JOptionPane.showMessageDialog(this, "Project could not be found.",
                        "Project not found", JOptionPane.WARNING_MESSAGE);
                log.error("Project {} not found, not selecting", selectedProject);
                return;
            }

            log.info("Initializing DB connection...");
            DBMySQL db = (DBMySQL) ADatabase.of(
                    project.getServer().split(":")[0],
                    Integer.parseInt((project.getServer().contains(":") ? project.getServer().split(":")[1] : "3306")),
                    project.getUsername(),
                    project.getPassword(),
                    project.getDefaultDatabase()
            );
            if (db == null) {
                log.warn("Database object is null");
                return;
            }
            log.info("Connecting to database {}...", project.getServer());
            JDialog dialog = new JDialog();
            //dialog.setUndecorated(true);
            //dialog.setAlwaysOnTop(true);
            //dialog.setLocationRelativeTo(this);
            //dialog.setModal(true);
            //dialog.setTitle("Select Database");
            //dialog.add(new JLabel("Loading database..."));
            //dialog.setVisible(true);
            log.info("DB connection loaded");
            if (db.connect()) {
                log.info("DB connection established");
                db.getDatabases().forEach(database -> {
                    log.info("Found database {}", database);
                });
                dialog.dispose();
            } else {
                log.warn("DB connection failed");
                JOptionPane.showMessageDialog(dialog, "Database connection failed.",
                        "Database connection failed", JOptionPane.ERROR_MESSAGE);
                dialog.dispose();
            }
            database = db;
        });

        JTextField pathField = new JTextField();
        JButton browseButton = new JButton("Browse");

        panel.add(new JLabel("Export path:"));
        panel.add(pathField);
        panel.add(new JLabel(""));
        panel.add(browseButton);

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setFileFilter(new FileNameExtensionFilter("DBX export file", "dbx"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                if (!chooser.getSelectedFile().getName().endsWith(".dbx")) selectedFile = new File(chooser.getSelectedFile().getAbsolutePath() + ".dbx");
                pathField.setText(selectedFile.getAbsolutePath());

                try {
                    writer = new DbxArchiveWriter(selectedFile.toPath());

                    startButton.addActionListener(el ->
                            startExport(writer, database)
                    );
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    log.error(ex.getMessage(), ex);
                }
            }
        });

        panel.add(projectComboBox);

        return panel;
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

    private void startExport(
            DbxArchiveWriter writer,
            ADatabase database) {
        startButton.setEnabled(false);
        console.setText("");

        ExportListener listener = createListener();

        new Thread(() -> {
            try {
                DatabaseExporter exporter =
                        new DatabaseExporter(writer, database, listener, ExportDialog.this);

                exporter.retrieveBasicData();
                exporter.createManifest();
                exporter.exportData();

                listener.onLog("Export completed successfully.");
            } catch (Exception e) {
                listener.onError("Export failed", e);
                log.error(e.getMessage(), e);
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.error("Failed to close Stream");
                    log.error(e.getMessage(), e);
                }
                SwingUtilities.invokeLater(() ->
                        startButton.setEnabled(true)
                );
            }
        }, "dbx-export-thread").start();
    }

    private ExportListener createListener() {
        return new ExportListener() {

            @Override
            public void onProgress(int current, int total, String message) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setMaximum(total);
                    progressBar.setValue(current);
                    progressBar.setString(message + " (" + current + "/" + total + ")");
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
        };
    }
}
