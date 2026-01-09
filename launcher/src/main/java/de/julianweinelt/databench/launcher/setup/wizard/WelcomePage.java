package de.julianweinelt.databench.launcher.setup.wizard;

import de.julianweinelt.databench.launcher.setup.SetupState;
import de.julianweinelt.databench.launcher.setup.WizardPage;

import javax.swing.*;
import java.awt.*;

public class WelcomePage implements WizardPage {

    private final SetupState state;
    private JPanel panel;

    public WelcomePage(SetupState state) {
        this.state = state;
        initUI();
    }

    private void initUI() {
        panel = new JPanel(new BorderLayout(0, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        panel.setBackground(UIManager.getColor("Panel.background"));

        // Titel
        JLabel title = new JLabel("Welcome to DataBench");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        panel.add(title, BorderLayout.NORTH);

        // Bullet points
        JPanel bullets = new JPanel();
        bullets.setLayout(new BoxLayout(bullets, BoxLayout.Y_AXIS));
        bullets.setBackground(panel.getBackground());

        bullets.add(createBullet("Configure language, drivers, and runtime"));
        bullets.add(createBullet("Optimized defaults for developers"));
        bullets.add(createBullet("Settings can be changed later"));

        panel.add(bullets, BorderLayout.CENTER);

        // Optional checkbox: Start after setup
        JCheckBox startNow = new JCheckBox("Start DataBench automatically after setup", true);
        startNow.setBackground(panel.getBackground());
        startNow.addActionListener(e -> state.startAfterFinish = startNow.isSelected());
        panel.add(startNow, BorderLayout.SOUTH);
    }

    private JLabel createBullet(String text) {
        JLabel label = new JLabel("• " + text);
        label.setFont(label.getFont().deriveFont(16f));
        label.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        return label;
    }

    @Override
    public String getId() {
        return "welcome";
    }

    @Override
    public JComponent getView() {
        return panel;
    }

    @Override
    public boolean canGoNext() {
        return true; // immer Next möglich
    }
}
