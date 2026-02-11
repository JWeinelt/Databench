package de.julianweinelt.databench.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.IntelliJTheme;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import de.julianweinelt.databench.DataBench;
import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.plugins.DbxPlugin;
import de.julianweinelt.databench.dbx.api.ui.theme.Theme;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.io.InputStream;

@Slf4j
public class ThemeSwitcher {

    public static void switchTheme(String themeName, DbxPlugin plugin) {
        log.debug("Switching to theme {}:{}", plugin.getName(), themeName);
        Theme t = Registry.instance().getTheme(themeName);
        if (t == null) {
            log.warn("No theme with name {} found", themeName);
            JOptionPane.showMessageDialog(null, "The theme \"" + themeName + "\" could not be found." +
                            "\nMaybe the plugin providing it could not be loaded.\n\nUsing the default dark theme instead.",
                    "Theme not found", JOptionPane.ERROR_MESSAGE);
            ThemeSwitcher.switchTheme("dark", Registry.instance().getPlugin("system"));
            return;
        }
        if (t.getLafClass() == null) {
            updateLaf(createIntelliJLaf(themeName, plugin));
        } else {
            updateLaf(t.getLafClass());
        }
    }

    private static FlatLaf createIntelliJLaf(String name, DbxPlugin plugin) {
        String resourcePath = "/themes/" + name + ".theme.json";
        try (InputStream is = plugin.getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Theme not found: " + resourcePath);
            }

            return IntelliJTheme.createLaf(is);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load theme", ex);
        }
    }

    private static void updateLaf(BasicLookAndFeel laf) {
        FlatAnimatedLafChange.showSnapshot();
        try {
            UIManager.setLookAndFeel(laf);

            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
            DataBench.getInstance().getUi().updateProjectCards();
        } catch (UnsupportedLookAndFeelException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Deprecated(forRemoval = true)
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
}
