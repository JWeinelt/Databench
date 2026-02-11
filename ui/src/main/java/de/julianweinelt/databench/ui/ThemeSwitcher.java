package de.julianweinelt.databench.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import de.julianweinelt.databench.DataBench;

import javax.swing.*;
import java.awt.*;

public class ThemeSwitcher {

    public static void switchTheme(FlatLaf newLaf) {
        FlatAnimatedLafChange.showSnapshot();
        try {
            UIManager.setLookAndFeel(newLaf);

            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
            DataBench.getInstance().getUi().updateProjectCards();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    public static void updateAllWindows() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.pack();
        }
    }
}
