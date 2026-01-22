package de.julianweinelt.databench.dbx.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.events.Event;
import de.julianweinelt.databench.dbx.api.events.Subscribe;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;import java.nio.charset.StandardCharsets;import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class LanguageManager {
    private final int parserVersion = 1;
    private JsonObject preLoadedLangData = new JsonObject();

    private static LanguageManager instance;

    public LanguageManager() {
        instance = this;
        Registry.instance().registerListener(this, Registry.instance().getSystemPlugin());
    }

    public static LanguageManager instance() {
        return instance;
    }
    public static String translate(String key, Map<String, String> placeholders) {
        return instance().getTranslation(key, placeholders);
    }
    public static String translate(String key) {
        return instance().getTranslation(key, Map.of());
    }

    public String fromFriendlyName(String friendlyName) {
        File[] files = new File("locale").listFiles();
        if (files == null) {
            log.warn("Locale folder is empty.");
            return "en_us";
        }
        log.info("Searching for id of {}...", friendlyName);
        for (File file : files) {
            log.info("Inspecting {}", file.getName());
            JsonObject o = loadData(file.getName());
            log.info(o.toString());
            if (o.has("metaData")) {
                if (o.get("metaData").getAsJsonObject().get("language").getAsString().equals(friendlyName)) return friendlyName;
                if (o.get("metaData").getAsJsonObject().get("friendlyName").getAsString().equals(friendlyName)) {
                    int fileVersion = o.get("metaData").getAsJsonObject().get("fileVersion").getAsInt();
                    if (fileVersion != parserVersion) {
                        log.warn("Loaded file for language {}, which has fileVersion {} (Parser Version {}), it may not work correctly (Version mismatch).",
                                file.getName(), fileVersion, parserVersion);
                    }
                    return o.get("metaData").getAsJsonObject().get("language").getAsString();
                }
            } else log.warn("File {} does not have any metadata.", file.getName());
        }
        log.warn("No id found for {}", friendlyName);
        return "en_us";
    }

    public String fromID(String id) {
        File[] files = new File("locale").listFiles();
        if (files == null) {
            log.warn("Locale folder is empty.");
            return "English (US)";
        }
        log.debug("Searching for friendly name of {}...", id);
        for (File file : files) {
            log.debug("Inspecting {}", file.getName());
            JsonObject o = loadData(file.getName());
            log.info(o.toString());
            if (o.has("metaData")) {
                if (o.get("metaData").getAsJsonObject().get("friendlyName").getAsString().equals(id)) return id;
                if (o.get("metaData").getAsJsonObject().get("language").getAsString().equals(id)) {
                    int fileVersion = o.get("metaData").getAsJsonObject().get("fileVersion").getAsInt();
                    if (fileVersion != parserVersion) {
                        log.warn("Loaded file for language {}, which has fileVersion {} (Parser Version {}), it may not work correctly (Version mismatch).",
                                file.getName(), fileVersion, parserVersion);
                    }
                    return o.get("metaData").getAsJsonObject().get("friendlyName").getAsString();
                }
            } else log.warn("File {} does not have any metadata.", file.getName());
        }
        log.warn("No friendly name found for {}", id);
        return "English (US)";
    }

    public String getTranslation(String key, Map<String, String> placeholders) {
        JsonElement e = preLoadedLangData.get(key);
        if (e == null) return key;
        String dat = e.getAsString();
        for (String placeholder : placeholders.keySet()) {
            dat = dat.replace("${" + placeholder + "}", placeholders.get(placeholder));
        }
        return dat;
    }

    public void copyLanguageDataIfNotExists() {
        Type type = new TypeToken<List<String>>(){}.getType();
        if (new File("locale").mkdirs()) log.debug("Created locale dir");

        try (InputStream is = getClass().getResourceAsStream("/lang/preloaded.json")) {
             if (is == null) return;
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);

            List<String> languages = new Gson().fromJson(reader, type);
            for (String l : languages) {
                if (!new File("locale", l + ".json").exists()) {
                    try {
                        InputStream iS2 = getClass().getResourceAsStream("/lang/" + l + ".json");
                        if (iS2 == null) return;
                        Files.copy(iS2, new File("locale",  l + ".json").toPath());
                    } catch (IOException e) {
                        log.error("Error copying en_us locale file.");
                    } catch (NullPointerException e) {
                        log.error("Default locale file {} not found.", l);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public CompletableFuture<Void> preload(String currentLocale) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        File file = new File("locale", currentLocale + ".json");
        if (!file.exists()) {
            log.warn("Locale file for {} does not exist. Defaulting to en_us.", currentLocale);
            file = new File("locale", "en_us.json");
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            preLoadedLangData = JsonParser.parseReader(br).getAsJsonObject();
            callChangeLangEvent(currentLocale);
            future.complete(null);
        } catch (IOException e) {
            log.warn("Failed to load locale file. Defaulting to en_us.", e);
            callChangeLangEvent("en_us");
            preload("en_us");
            preLoadedLangData = new JsonObject();
            future.completeExceptionally(e);
        } catch (JsonSyntaxException e) {
            log.error(e.getMessage(), e);
            log.error("Try to delete the locale file for {} and restart the application.", currentLocale);
        }
        return future;
    }

    public List<String> getFriendlyNames() {
        File[] files = new File("locale").listFiles();
        if (files == null) return new ArrayList<>();
        List<String> friendlyNames = new ArrayList<>();
        for (File file : files) {
            log.info("Inspecting {}", file.getName());
            JsonObject o = loadData(file.getName());
            if (o.has("metaData")) {
                int fileVersion = o.get("metaData").getAsJsonObject().get("fileVersion").getAsInt();
                if (fileVersion != parserVersion) {
                    log.warn("Loaded file for language {}, which has fileVersion {} (Parser Version {}), it may not work correctly.",
                            file.getName(), fileVersion, parserVersion);
                }
                String friendlyName = o.get("metaData").getAsJsonObject().get("friendlyName").getAsString();
                friendlyNames.add(friendlyName);
            } else log.warn("File {} does not have any metadata.", file.getName());
        }
        return friendlyNames;
    }

    private JsonObject loadData(String lang) {
        File file = new File("locale", lang);
        if (!file.exists()) {
            log.warn("Locale file for {} does not exist. Defaulting to en_us.", lang);
            return new JsonObject();
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return JsonParser.parseReader(br).getAsJsonObject();
        } catch (IOException e) {
            log.warn("Failed to load locale file. Defaulting to en_us.", e);
            callChangeLangEvent("en_us");
            preload("en_us");
            return new JsonObject();
        }
    }

    private void callChangeLangEvent(String locale) {
        Registry.instance().callEvent(new Event("LanguageChangeEvent").set("locale", locale));
    }

    @Subscribe(value = "PluginLanguageDataEvent")
    public void onTranslationAdd(Event event) {
        log.info("Loading language data for plugin {}", event.get("plugin").asString());
        JsonObject b = JsonParser.parseString(event.get("languageData").asString()).getAsJsonObject();
        JsonObject data = new JsonObject();
        String lang = "en_us"; //TODO: Read it
        if (b.has(lang)) data = b.get(lang).getAsJsonObject();
        else b.get("en_us").getAsJsonObject();

        for (String key : data.keySet()) {
            preLoadedLangData.addProperty(key, data.get(key).getAsString());
        }
    }

    @Subscribe(value = "LanguageChangeEvent")
    public void onLanguageChange(Event e) {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
        log.info("Language changed");
    }
}
