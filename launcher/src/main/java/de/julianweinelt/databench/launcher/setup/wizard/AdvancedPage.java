package de.julianweinelt.databench.launcher.setup.wizard;

import de.julianweinelt.databench.launcher.setup.SetupState;
import de.julianweinelt.databench.launcher.setup.WizardPage;

import javax.swing.*;
import java.util.List;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Component;
import java.io.File;

public class AdvancedPage implements WizardPage {

    private final SetupState state;
    private JPanel panel;
    private JTextField javaPathField;
    private JTextArea jvmArgsArea;
    private JTextArea startupArgsArea;

    public AdvancedPage(SetupState state) {
        this.state = state;
        initUI();
    }

    private void initUI() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        panel.setBackground(UIManager.getColor("Panel.background"));

        JLabel title = new JLabel("Advanced Settings");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(20));

        // Java Path
        JPanel javaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        javaPanel.setBackground(panel.getBackground());
        javaPathField = new JTextField(30);
        javaPathField.setText(state.javaPath != null ? state.javaPath : "");
        JButton browseBtn = new JButton("Browse");
        browseBtn.addActionListener(e -> browseJava());
        JButton systemBtn = new JButton("Use System Java");
        systemBtn.addActionListener(e -> useSystemJava());

        javaPanel.add(new JLabel("Java Path:"));
        javaPanel.add(javaPathField);
        javaPanel.add(browseBtn);
        javaPanel.add(systemBtn);

        panel.add(javaPanel);
        panel.add(Box.createVerticalStrut(20));

        // JVM Args
        panel.add(new JLabel("JVM Arguments (one per line):"));
        jvmArgsArea = new JTextArea(5, 50);
        jvmArgsArea.setText(String.join("\n", state.jvmArgs.isEmpty() ? List.of("-Xms256m", "-Xmx2048m") : state.jvmArgs));
        panel.add(new JScrollPane(jvmArgsArea));
        panel.add(Box.createVerticalStrut(20));

        // Startup Args
        panel.add(new JLabel("Startup Arguments:"));
        startupArgsArea = new JTextArea(3, 50);
        panel.add(new JScrollPane(startupArgsArea));
    }

    private void browseJava() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = chooser.showOpenDialog(panel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            javaPathField.setText(f.getAbsolutePath());
        }
    }

    private void useSystemJava() {
        state.javaPath = null; // signalisiert: system Java verwenden
        javaPathField.setText("System Java will be used");
    }

    @Override
    public String getId() {
        return "advanced";
    }

    @Override
    public JComponent getView() {
        return panel;
    }

    @Override
    public boolean canGoNext() {
        state.javaPath = javaPathField.getText().trim().isEmpty() ? null : javaPathField.getText().trim();
        state.jvmArgs = List.of(jvmArgsArea.getText().split("\\n"));
        // Startup args optional
        return true;
    }
}
