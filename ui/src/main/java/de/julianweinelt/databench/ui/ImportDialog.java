package de.julianweinelt.databench.ui;

import de.julianweinelt.databench.data.Project;
import de.julianweinelt.databench.data.ProjectManager;
import de.julianweinelt.databench.dbx.database.ADatabase;
import de.julianweinelt.databench.dbx.export.DbxArchiveReader;
import de.julianweinelt.databench.dbx.export.ImportListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class ImportDialog extends JDialog implements ImportListener {

    private final JTextField archiveField = new JTextField();
    private final JTextField targetField = new JTextField();
    private final JTextArea logArea = new JTextArea();
    private final JProgressBar progressBar = new JProgressBar();

    private final JButton startButton = new JButton("Start Import");
    private final JButton cancelButton = new JButton("Cancel");
    private final JButton browseButton = new JButton("...");

    private ADatabase targetDatabase = null;

    public ImportDialog(Frame owner) {
        super(owner, "Import DBX Archive", true);

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
        projects.addItem("Select Project");
        ProjectManager.instance().getProjects().forEach(project -> projects.addItem(project.getName()));
        projects.addActionListener(e -> {
            String selected = (String) projects.getSelectedItem();
            if (selected == null) return;
            if (selected.equals("Select Project")) return;

            Project project = ProjectManager.instance().getProject(selected);

            targetDatabase = ADatabase.of(
                    project.getServer().split(":")[0],
                    (project.getServer().split(":").length == 1) ? 3306 : Integer.parseInt(project.getServer().split(":")[1]),
                    project.getUsername(),
                    project.getPassword(),
                    project.getDefaultDatabase()
            );
            if (!archiveField.getText().isEmpty())
                startButton.setEnabled(true);
            targetField.setText(project.getName());

        });
        top.add(projects, c);

        c.gridx = 0; c.gridy = 1;
        top.add(new JLabel("Archive file:"), c);

        c.gridx = 1; c.weightx = 1;
        top.add(archiveField, c);

        c.gridx = 2; c.weightx = 0;
        top.add(browseButton, c);

        c.gridx = 0; c.gridy = 2;
        top.add(new JLabel("Target DB:"), c);

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
        cancelButton.addActionListener(e -> dispose());
        startButton.addActionListener(e -> startImport());
    }

    private void chooseArchive() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("DBX archive files", "dbx"));

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
                    "Please select an archive file",
                    "Missing file",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        startButton.setEnabled(false);
        browseButton.setEnabled(false);
        logArea.setText("");
        progressBar.setValue(0);

        new Thread(() -> {
            try {
                DbxArchiveReader reader =
                        new DbxArchiveReader(new File(archiveField.getText()).toPath());

                DatabaseImporter importer =
                        new DatabaseImporter(reader, targetDatabase, this, this);

                importer.readManifest();
                importer.validate();
                importer.connectTarget();
                importer.loadSchemas();
                importer.importData();

            } catch (Exception ex) {
                onError("Import failed", ex);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(
                                this,
                                ex.getMessage(),
                                "Import failed",
                                JOptionPane.ERROR_MESSAGE
                        )
                );
            }
        }, "dbx-import-thread").start();
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onProgress(int current, int total, String message) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setMaximum(total);
            progressBar.setValue(current);
            progressBar.setStringPainted(true);
            progressBar.setString(message);
        });
    }

    @Override
    public void onError(String message, Throwable e) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[ERROR] " + message + "\n");
            if (e != null) {
                logArea.append("  " + e.getMessage() + "\n");
            }
        });
    }
}
