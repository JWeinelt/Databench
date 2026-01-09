package de.julianweinelt.databench.launcher.setup.wizard;

import de.julianweinelt.databench.launcher.setup.SetupState;
import de.julianweinelt.databench.launcher.setup.WizardPage;

import javax.swing.*;
import java.awt.*;

public class FinishPage implements WizardPage {

    private final SetupState state;
    private JPanel panel;
    private JCheckBox startCheck;

    public FinishPage(SetupState state) {
        this.state = state;
        initUI();
    }

    private void initUI() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        panel.setBackground(UIManager.getColor("Panel.background"));

        JLabel title = new JLabel("Setup Complete 🎉");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);

        panel.add(Box.createVerticalStrut(30));

        startCheck = new JCheckBox("Start DataBench now", true);
        startCheck.setAlignmentX(Component.CENTER_ALIGNMENT);
        startCheck.setBackground(panel.getBackground());
        panel.add(startCheck);
    }

    @Override
    public String getId() {
        return "finish";
    }

    @Override
    public JComponent getView() {
        return panel;
    }

    @Override
    public boolean canGoNext() {
        state.startAfterFinish = startCheck.isSelected();
        return true;
    }
}
