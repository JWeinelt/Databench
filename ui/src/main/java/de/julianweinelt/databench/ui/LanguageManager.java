package de.julianweinelt.databench.ui;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.databench.DataBench;
import de.julianweinelt.databench.data.Configuration;
import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.events.Event;
import de.julianweinelt.databench.dbx.api.events.Priority;
import de.julianweinelt.databench.dbx.api.events.Subscribe;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class LanguageManager {
    private final int parserVersion = 1;
    private JsonObject preLoadedLangData = new JsonObject();

    public LanguageManager() {
        Registry.instance().registerListener(this, Registry.instance().getSystemPlugin());
    }

    public static LanguageManager instance() {
        return DataBench.getInstance().getLanguageManager();
    }
    public static String translate(String key, Map<String, String> placeholders) {
        return instance().getTranslation(key, placeholders);
    }
    public static String translate(String key) {
        return instance().getTranslation(key, Map.of());
    }

    public String fromFriendlyName(String friendlyName) {
        File[] files = new File("locale").listFiles();
        if (files == null) return "en_us";
        for (File file : files) {
            JsonObject o = loadData(file.getName().replace(".json", ""));
            if (o.has("metaData") && o.get("metaData").getAsJsonObject().get("friendlyName").getAsString().equals(friendlyName)) {
                int fileVersion = o.get("metaData").getAsJsonObject().get("fileVersion").getAsInt();
                if (fileVersion != parserVersion) {
                    log.warn("Loaded file for language {}, which has fileVersion {} (Parser Version {}), it may not work correctly.",
                            file.getName(), fileVersion, parserVersion);
                }
                return o.get("metaData").getAsJsonObject().get("language").getAsString();
            }
        }
        return "en_us";
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

    public CompletableFuture<Void> preload() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        new File("locale").mkdirs();
        String selectedLang = Configuration.getConfiguration().getLocale();

        if (!new File("locale", "en_us.json").exists()) {
            try {
                Files.copy(getClass().getResourceAsStream("/lang/en_us.json"), new File("locale", "en_us.json").toPath());
            } catch (IOException e) {
                log.error("Error copying en_us locale file.");
                return CompletableFuture.completedFuture(null);
            } catch (NullPointerException e) {
                log.error("Default locale file en_us not found.");
                return CompletableFuture.completedFuture(null);
            }
        }

        File file = new File("locale", selectedLang + ".json");
        if (!file.exists()) {
            log.warn("Locale file for {} does not exist. Defaulting to en_us.", selectedLang);
            file = new File("locale", "en_us.json");
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            preLoadedLangData = JsonParser.parseReader(br).getAsJsonObject();
            future.complete(null);
        } catch (IOException e) {
            log.warn("Failed to load locale file. Defaulting to en_us.", e);
            Configuration.getConfiguration().setLocale("en_us");
            preload();
            preLoadedLangData = new JsonObject();
            future.completeExceptionally(e);
        } catch (JsonSyntaxException e) {
            log.error(e.getMessage(), e);
            log.error("Try to delete the locale file for {} and restart the application.", selectedLang);
        }
        return future;
    }

    public List<String> getFriendlyNames() {
        File[] files = new File("locale").listFiles();
        if (files == null) return new ArrayList<>();
        List<String> friendlyNames = new ArrayList<>();
        for (File file : files) {
            JsonObject o = loadData(file.getName().replace(".json", ""));
            if (o.has("metaData")) {
                int fileVersion = o.get("metaData").getAsJsonObject().get("fileVersion").getAsInt();
                if (fileVersion != parserVersion) {
                    log.warn("Loaded file for language {}, which has fileVersion {} (Parser Version {}), it may not work correctly.",
                            file.getName(), fileVersion, parserVersion);
                }
                String friendlyName = o.get("metaData").getAsJsonObject().get("friendlyName").getAsString();
                friendlyNames.add(friendlyName);
            }
        }
        return friendlyNames;
    }

    private JsonObject loadData(String lang) {
        File file = new File("locale", lang + ".json");
        if (!file.exists()) {
            log.warn("Locale file for {} does not exist. Defaulting to en_us.", lang);
            return new JsonObject();
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return JsonParser.parseReader(br).getAsJsonObject();
        } catch (IOException e) {
            log.warn("Failed to load locale file. Defaulting to en_us.", e);
            Configuration.getConfiguration().setLocale("en_us");
            preload();
            return new JsonObject();
        }
    }

    @Subscribe(value = "PluginLanguageDataEvent")
    public void onTranslationAdd(Event event) {
        log.info("Loading language data for plugin {}", event.get("plugin").asString());
        String lang = Configuration.getConfiguration().getLocale();
        JsonObject b = JsonParser.parseString(event.get("languageData").asString()).getAsJsonObject();
        JsonObject data = new JsonObject();
        if (b.has(lang)) data = b.get(lang).getAsJsonObject();
        else b.get("en_us").getAsJsonObject();

        for (String key : data.keySet()) {
            preLoadedLangData.addProperty(key, data.get(key).getAsString());
        }
    }
}
