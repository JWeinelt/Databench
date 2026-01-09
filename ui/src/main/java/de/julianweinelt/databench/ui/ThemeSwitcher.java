package de.julianweinelt.databench.ui;

import com.formdev.flatlaf.FlatLaf;
import javax.swing.*;
import java.awt.*;

public class ThemeSwitcher {

    public static void switchTheme(FlatLaf newLaf) {
        try {
            UIManager.setLookAndFeel(newLaf);

            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
                window.pack();
            }
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
