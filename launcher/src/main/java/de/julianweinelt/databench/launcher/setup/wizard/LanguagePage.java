package de.julianweinelt.databench.launcher.setup.wizard;

import de.julianweinelt.databench.launcher.setup.SetupState;
import de.julianweinelt.databench.launcher.setup.WizardPage;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LanguagePage implements WizardPage {

    private final SetupState state;
    private JPanel panel;
    private List<JCheckBox> checkboxes;

    private final String[] availableLanguages = {"English", "Deutsch", "Français"};

    public LanguagePage(SetupState state) {
        this.state = state;
        initUI();
    }

    private void initUI() {
        panel = new JPanel(new BorderLayout(10, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        panel.setBackground(UIManager.getColor("Panel.background"));

        JLabel title = new JLabel("Select Languages");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        panel.add(title, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(panel.getBackground());

        checkboxes = new java.util.ArrayList<>();
        for (String lang : availableLanguages) {
            JCheckBox cb = new JCheckBox(lang);
            cb.setFont(cb.getFont().deriveFont(16f));
            cb.setBackground(panel.getBackground());
            if (lang.equals("English")) cb.setSelected(true); // default
            checkboxes.add(cb);
            listPanel.add(cb);
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public String getId() {
        return "languages";
    }

    @Override
    public JComponent getView() {
        return panel;
    }

    @Override
    public boolean canGoNext() {
        // mindestens eine Sprache muss ausgewählt sein
        boolean valid = checkboxes.stream().anyMatch(JCheckBox::isSelected);
        if (valid) {
            state.selectedLanguages.clear();
            checkboxes.stream().filter(JCheckBox::isSelected)
                    .forEach(cb -> state.selectedLanguages.add(cb.getText()));
        }
        return valid;
    }

    @Override
    public void onEnter() {
        // optional: Fokus auf ScrollPane setzen
    }
}
