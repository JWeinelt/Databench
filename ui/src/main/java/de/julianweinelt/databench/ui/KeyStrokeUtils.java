package de.julianweinelt.databench.ui;

import javax.swing.*;
import java.awt.event.KeyEvent;

public final class KeyStrokeUtils {

    public static String toString(KeyStroke ks) {
        if (ks == null) return "";
        return KeyEvent.getKeyModifiersText(ks.getModifiers()) +
                (ks.getModifiers() == 0 ? "" : "+") +
                KeyEvent.getKeyText(ks.getKeyCode());
    }
}
