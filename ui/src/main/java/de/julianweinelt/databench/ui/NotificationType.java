package de.julianweinelt.databench.ui;

import java.awt.*;

public enum NotificationType {

    INFO(
            new Color(104, 151, 187),
            5000
    ),

    TIP(
            new Color(152, 195, 121),
            7000
    ),

    WARNING(
            new Color(209, 154, 102),
            0
    ),

    ERROR(
            new Color(224, 108, 117),
            0
    );

    private final Color accentColor;
    private final int autoCloseMillis;

    NotificationType(Color accentColor, int autoCloseMillis) {
        this.accentColor = accentColor;
        this.autoCloseMillis = autoCloseMillis;
    }

    public Color accentColor() {
        return accentColor;
    }

    public int autoCloseMillis() {
        return autoCloseMillis;
    }

    public boolean hasAutoClose() {
        return autoCloseMillis > 0;
    }
}
