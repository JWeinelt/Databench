package de.julianweinelt.datacat.server.util;

import java.awt.*;
import java.util.Optional;

public class ColorUtil {
    public static Color fromText(String input) {
        String[] args = input.split(";");
        for (String s : args) {
            if (parseInt(s).isEmpty()) return null;
        }
        if (args.length == 3) {
            return new Color(
                    parseInt(args[0]).orElse(0),
                    parseInt(args[1]).orElse(0),
                    parseInt(args[2]).orElse(0)
            );
        } else if (args.length == 4) {
            return new Color(
                    parseInt(args[0]).orElse(0),
                    parseInt(args[1]).orElse(0),
                    parseInt(args[2]).orElse(0),
                    parseInt(args[3]).orElse(0)
            );
        } else return null;
    }

    public static String toText(Color color) {
        return formatInt(color.getRed()) + ";"
                + formatInt(color.getGreen()) + ";"
                + formatInt(color.getBlue()) + ";"
                + formatInt(color.getAlpha()) + ";";
    }

    private static String formatInt(int input) {
        if (input < 10) return "00" + input;
        if (input < 100) return "0" + input;
        if (input < 255) return "" + input;
        return "000";
    }

    private static Optional<Integer> parseInt(String input) {
        try {
            return Optional.of(Integer.parseInt(input));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
