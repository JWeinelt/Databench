package de.julianweinelt.databench.data;

import de.julianweinelt.databench.DataBench;
import de.julianweinelt.databench.api.DatabaseType;
import de.julianweinelt.databench.ui.BenchUI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
public class ProjectManager {

    private final List<Project> projects = new ArrayList<>();
    private final File projectsDir = new File("projects");

    public static Project LIGHT_EDIT_PROJECT = new Project(UUID.fromString("94534fe5-771e-430e-97fe-58b78d4c79af")
            , "Light Edit", "", "", "", "", false, DatabaseType.MYSQL);

    public ProjectManager() {
        projectsDir.mkdirs();
    }

    public static ProjectManager instance() {
        return DataBench.getInstance().getProjectManager();
    }

    public void addProject(Project project, String password) throws Exception {
        projects.add(project);
        saveProjectFile(project, password);
    }
    public void removeProject(Project project) {
        projects.remove(project);

    }

    public void saveProjectFile(Project project, String password) throws Exception {
        File file = new File(projectsDir, project.getName() + ".dbproj");
        ProjectEncryptionUtil.encryptProject(project, file, password);
    }

    public void deleteProjectFile(Project project, BenchUI ui) {
        File file = new File(projectsDir, project.getName() + ".dbproj");
        if (file.delete()) {
            JOptionPane.showMessageDialog(ui.getFrame(), "The project \""
                    + project.getName() + "\" has been deleted.", "Information", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(ui.getFrame(), "Failed to delete the project \"" + project.getName()
                    + "\".\n\nPlease check if you have the permission to do that.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadAllProjects(String defaultPassword) {
        File[] files = projectsDir.listFiles((dir, name) -> name.endsWith(".dbproj"));
        if (files == null) return;

        for (File f : files) {
            try {
                Project p = ProjectEncryptionUtil.decryptProject(f, defaultPassword, Project.class);
                projects.add(p);
            } catch (Exception e) {
                System.err.println("Failed to load project " + f.getName() + ": " + e.getMessage());
            }
        }
    }


    public void exportProject(Project project, File targetFile, String password) throws Exception {
        ProjectEncryptionUtil.encryptProject(project, targetFile, password);
    }

    public void showExportPopup(BenchUI ui, Project project) {
        String name = project.getName();
        JFrame popup = new JFrame("Export Project: " + name);
        popup.setLayout(new GridBagLayout());
        popup.setSize(450, 180);
        popup.setResizable(false);
        popup.setLocationRelativeTo(ui.getFrame());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel passwordLabel = new JLabel("Encryption Password (optional):");
        popup.add(passwordLabel, gbc);

        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField();
        popup.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        JButton chooseFileButton = new JButton("Choose Export File...");
        popup.add(chooseFileButton, gbc);

        JLabel chosenFileLabel = new JLabel("No file selected");
        gbc.gridy = 2;
        popup.add(chosenFileLabel, gbc);

        final File[] selectedFile = {null};
        chooseFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Project");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setSelectedFile(new File(name + ".dbproj"));
            int result = fileChooser.showSaveDialog(popup);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = fileChooser.getSelectedFile();
                chosenFileLabel.setText(selectedFile[0].getAbsolutePath());
            }
        });

        gbc.gridy = 3;
        JButton exportButton = new JButton("Export");
        popup.add(exportButton, gbc);

        exportButton.addActionListener(e -> {
            if (selectedFile[0] == null) {
                JOptionPane.showMessageDialog(popup, "Please choose a file to export.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String password = new String(passwordField.getPassword());
            try {
                exportProject(project, selectedFile[0], password);
                JOptionPane.showMessageDialog(popup, "Project exported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                popup.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(popup, "Failed to export project: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        popup.setVisible(true);
    }



    public boolean projectExists(String name) {
        return projects.stream().anyMatch(p -> p.getName().equals(name));
    }


    public void showEditProjectPopup(Project project, BenchUI ui) {
        JFrame frame = ui.getFrame();
        JDialog popup = new JDialog(frame, "Edit Project: " + project.getName(), true);
        popup.setResizable(false);
        popup.setSize(400, 350);
        popup.setLayout(new GridBagLayout());
        popup.setLocationRelativeTo(frame);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        popup.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        JTextField usernameField = new JTextField(project.getUsername(), 20);
        popup.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        popup.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(project.getPassword(), 20);
        popup.add(passwordField, gbc);

        String host = project.getServer().split(":")[0];
        int port = (project.getServer().split(":").length == 1) ? 3306 : Integer.parseInt(project.getServer().split(":")[1]);

        gbc.gridx = 0;
        gbc.gridy++;
        popup.add(new JLabel("Server Host:"), gbc);
        gbc.gridx = 1;
        JTextField hostField = new JTextField(host, 20);
        popup.add(hostField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        popup.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        JTextField portField = new JTextField(String.valueOf(port), 8);
        popup.add(portField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        popup.add(new JLabel("Default Database:"), gbc);
        gbc.gridx = 1;
        JTextField defaultDbField = new JTextField(project.getDefaultDatabase(), 20);
        popup.add(defaultDbField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        popup.add(new JLabel("Use SSL:"), gbc);
        gbc.gridx = 1;
        JCheckBox sslCheck = new JCheckBox();
        sslCheck.setSelected(project.isUseSSL());
        popup.add(sslCheck, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        popup.add(buttonPanel, gbc);

        saveButton.addActionListener(e -> {
            if (usernameField.getText().isBlank()) {
                JOptionPane.showMessageDialog(popup, "Username cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (hostField.getText().isBlank()) {
                JOptionPane.showMessageDialog(popup, "Host cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!portField.getText().matches("\\d+")) {
                JOptionPane.showMessageDialog(popup, "Port must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            project.setUsername(usernameField.getText());
            project.setPassword(new String(passwordField.getPassword()));
            project.setServer(hostField.getText() + ":" + portField.getText());
            project.setDefaultDatabase(defaultDbField.getText());
            project.setUseSSL(sslCheck.isSelected());

            try {
                ProjectManager.instance().saveProjectFile(project, Configuration.getConfiguration().getEncryptionPassword());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(popup, "Error while editing project.", "Error", JOptionPane.ERROR_MESSAGE);
                log.error(ex.getMessage(), ex);
            }

            popup.dispose();
        });

        cancelButton.addActionListener(e -> popup.dispose());

        popup.setVisible(true);
    }
}