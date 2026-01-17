package de.julianweinelt.databench.data;

import de.julianweinelt.databench.DataBench;
import de.julianweinelt.databench.dbx.util.ColorUtil;
import de.julianweinelt.databench.dbx.util.HomeDirectories;
import lombok.Getter;
import lombok.Setter;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class Configuration {
    private final UUID installationID = UUID.randomUUID();
    private String selectedTheme;
    private String locale;

    private String encryptionPassword;

    private Map<String, String> shortcuts = new HashMap<>();

    private boolean stoppedMaximized = false;

    private boolean sendAnonymousData = false;
    private boolean sendErrorProtocols = false;
    private boolean firstStartup = true;
    private boolean openProjectOnStartup = true;
    private String closeType = "ask";

    private int editorFontSize = 13;
    private int generalFontSize = 13;
    private int projectTreeFontSize = 13;

    private String editorFont = "Consolas";
    private String editorTheme = "software";
    private ColorSettings editorColors = new ColorSettings();

    private final HashMap<String, String> homeDirectories = new HashMap<>();

    private boolean checkForUpdates = true;
    private String updateChannel = "stable";
    private final int configVersion = 2;

    public static Configuration getConfiguration() {
        return DataBench.getInstance().getConfigManager().getConfiguration();
    }

    public void loadHomeDirectories() {
        homeDirectories.clear();
        for (String name : HomeDirectories.instance().names()) {
            homeDirectories.put(name, HomeDirectories.instance().get(name).getAbsolutePath());
        }
    }
    public void initHomeDirectories() {
        HomeDirectories.instance().clear();
        for (String name : homeDirectories.keySet()) {
            HomeDirectories.instance().put(name, homeDirectories.get(name));
        }
    }

    public KeyStroke getShortcut(String action, KeyStroke defaultKey) {
        String value = shortcuts.get(action);
        if (value == null || value.isBlank()) {
            return defaultKey;
        }
        KeyStroke ks = KeyStroke.getKeyStroke(value);
        return ks != null ? ks : defaultKey;
    }

    public void setShortcut(String action, KeyStroke stroke) {
        if (stroke == null) return;
        shortcuts.put(action, toPersistedString(stroke));
    }

    public void removeShortcut(String action) {
        shortcuts.remove(action);
    }

    public boolean hasCustomShortcut(String action) {
        return shortcuts.containsKey(action);
    }

    private String toPersistedString(KeyStroke ks) {
        StringBuilder sb = new StringBuilder();

        if ((ks.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0) sb.append("control ");
        if ((ks.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0) sb.append("shift ");
        if ((ks.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0) sb.append("alt ");
        if ((ks.getModifiers() & InputEvent.META_DOWN_MASK) != 0) sb.append("meta ");

        sb.append(KeyEvent.getKeyText(ks.getKeyCode()));
        return sb.toString().trim();
    }

    public Font getEditorFontObject() {
        return new Font(editorFont, Font.PLAIN, editorFontSize);
    }


    public static class ColorSettings {
        public static final Map<String, Integer> TOKEN_TYPES = Map.ofEntries(
            Map.entry("NULL", 0),
            Map.entry("COMMENT_EOL", 1),
            Map.entry("COMMENT_MULTILINE", 2),
            Map.entry("COMMENT_DOCUMENTATION", 3),
            Map.entry("COMMENT_KEYWORD", 4),
            Map.entry("COMMENT_MARKUP", 5),
            Map.entry("RESERVED_WORD", 6),
            Map.entry("RESERVED_WORD_2", 7),
            Map.entry("FUNCTION", 8),
            Map.entry("LITERAL_BOOLEAN", 9),
            Map.entry("LITERAL_NUMBER_DECIMAL_INT", 10),
            Map.entry("LITERAL_NUMBER_FLOAT", 11),
            Map.entry("LITERAL_NUMBER_HEXADECIMAL", 12),
            Map.entry("LITERAL_STRING_DOUBLE_QUOTE", 13),
            Map.entry("LITERAL_CHAR", 14),
            Map.entry("LITERAL_BACKQUOTE", 15),
            Map.entry("DATA_TYPE", 16),
            Map.entry("VARIABLE", 17),
            Map.entry("REGEX", 18),
            Map.entry("ANNOTATION", 19),
            Map.entry("IDENTIFIER", 20),
            Map.entry("WHITESPACE", 21),
            Map.entry("SEPARATOR", 22),
            Map.entry("OPERATOR", 23),
            Map.entry("PREPROCESSOR", 24),
            Map.entry("MARKUP_TAG_DELIMITER", 25),
            Map.entry("MARKUP_TAG_NAME", 26),
            Map.entry("MARKUP_TAG_ATTRIBUTE", 27),
            Map.entry("MARKUP_TAG_ATTRIBUTE_VALUE", 28),
            Map.entry("MARKUP_COMMENT", 29),
            Map.entry("MARKUP_DTD", 30),
            Map.entry("MARKUP_PROCESSING_INSTRUCTION", 31),
            Map.entry("MARKUP_CDATA_DELIMITER", 32),
            Map.entry("MARKUP_CDATA", 33),
            Map.entry("MARKUP_ENTITY_REFERENCE", 34),
            Map.entry("ERROR_IDENTIFIER", 35),
            Map.entry("ERROR_NUMBER_FORMAT", 36),
            Map.entry("ERROR_STRING_DOUBLE", 37),
            Map.entry("ERROR_CHAR", 38)
            //Map.entry("DEFAULT_NUM_TOKEN_TYPES", 39)
        );

        private final HashMap<Integer, String> colors = new HashMap<>();

        public List<String> getAppliedColors() {
            return new ArrayList<>(colors.values());
        }
        public Color getColorForName(String name) {
            for (Map.Entry<Integer, String> entry : colors.entrySet()) {
                if (entry.getValue().equals(name)) return ColorUtil.toColor(colors.get(entry.getKey()));
            }
            return null;
        }
        public int getKey(String name) {
            return TOKEN_TYPES.getOrDefault(name, -1);
        }

        public void setColor(int key, Color color) {
            colors.put(key, ColorUtil.toString(color));
        }
        public Color getColor(int key) {
            String color = colors.getOrDefault(key, null);
            if (color == null) return null;
            return ColorUtil.toColor(color);
        }
        public void removeColor(int key) {
            colors.remove(key);
        }
    }
}