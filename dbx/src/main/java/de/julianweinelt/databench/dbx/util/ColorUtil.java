package de.julianweinelt.databench.dbx.util;

import java.awt.*;

public class ColorUtil {
    public static String toString(Color color) {
        return color.getRed() + ";" + color.getGreen() +
                ";" + color.getBlue() + ";" + color.getAlpha();
    }

    public static Color toColor(String color) {
        String[] args = color.split(";");
        int red = Integer.parseInt(args[0]);
        int green = Integer.parseInt(args[1]);
        int blue = Integer.parseInt(args[2]);
        if (args.length == 3) {
            return new Color(red, green, blue);
        } else if (args.length == 4) {
            int alpha = Integer.parseInt(args[3]);
            return new Color(red, green, blue, alpha);
        } else {
            throw new IllegalArgumentException("Invalid color format");
        }
    }
}
