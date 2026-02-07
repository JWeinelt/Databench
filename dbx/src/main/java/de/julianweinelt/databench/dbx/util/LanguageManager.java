package de.julianweinelt.databench.dbx.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.databench.dbx.api.Registry;
import de.julianweinelt.databench.dbx.api.events.Event;
import de.julianweinelt.databench.dbx.api.events.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public final class LanguageManager {

    private static final int PARSER_VERSION = 1;
    private static LanguageManager instance;

    private final Map<String, LangMeta> metaById = new HashMap<>();
    private JsonObject activeTranslations = new JsonObject();
    private String currentLocale = "en_us";

    private boolean devMode = false;

    private record LangMeta(String id, String friendlyName, int version) {}

    public LanguageManager(boolean devMode) {
        this.devMode = devMode;
        instance = this;
        copyDefaultsIfMissing();
        loadAllMetaData();
        preload("en_us");
    }

    public static LanguageManager instance() {
        return instance;
    }

    public static String translate(String key) {
        return instance.translateInternal(key, Map.of());
    }

    public static String translate(String key, Map<String, String> placeholders) {
        return instance.translateInternal(key, placeholders);
    }

    public CompletableFuture<Void> preload(String locale) {
        return CompletableFuture.runAsync(() -> {
            String l = locale;
            JsonObject data = loadLocale(l);
            if (data.isEmpty() && !"en_us".equals(l)) {
                l = "en_us";
                data = loadLocale(l);
            }
            currentLocale = locale;
            activeTranslations = data;
            callChangeLangEvent(locale);
        });
    }

    public List<String> getFriendlyNames() {
        return metaById.values()
                .stream()
                .map(LangMeta::friendlyName)
                .sorted()
                .toList();
    }

    public String toId(String friendlyName) {
        for (LangMeta m : metaById.values()) if (m.friendlyName.equals(friendlyName) || m.id.equals(friendlyName)) return m.id;
        log.debug("Could not determine id of {}", friendlyName);
        return "en_us";
    }

    public String toFriendlyName(String id) {
        LangMeta meta = metaById.get(id);
        return meta != null ? meta.friendlyName : "English (US)";
    }

    private String translateInternal(String key, Map<String, String> placeholders) {
        JsonElement e = activeTranslations.get(key);
        if (e == null) return key;
        String result = e.getAsString();
        for (var entry : placeholders.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private void copyDefaultsIfMissing() {
        new File("locale").mkdirs();
        Type type = new TypeToken<List<String>>(){}.getType();

        try (InputStream is = getClass().getResourceAsStream("/lang/preloaded.json")) {
            if (is == null) return;

            List<String> langs = new Gson().fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8),
                    type
            );

            for (String lang : langs) {
                File target = new File("locale", lang + ".json");
                if (target.exists() && !devMode) continue;

                try (InputStream src = getClass().getResourceAsStream("/lang/" + lang + ".json")) {
                    if (src == null) continue;
                    Files.copy(src, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception e) {
            log.error("Failed to copy default languages", e);
        }
    }

    private void loadAllMetaData() {
        File[] files = new File("locale").listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (Reader r = new FileReader(file)) {
                JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                if (!root.has("metaData")) continue;

                JsonObject m = root.getAsJsonObject("metaData");
                LangMeta meta = new LangMeta(
                        m.get("language").getAsString(),
                        m.get("friendlyName").getAsString(),
                        m.get("fileVersion").getAsInt()
                );

                log.debug("Loaded meta for {} ({})", meta.id, meta.friendlyName);
                metaById.put(meta.id, meta);

                if (meta.version != PARSER_VERSION) {
                    log.warn("Locale {} has version {} but parser expects {}",
                            meta.id, meta.version, PARSER_VERSION);
                }
            } catch (Exception e) {
                log.warn("Failed to load metadata from {}", file.getName(), e);
            }
        }
    }

    private JsonObject loadLocale(String id) {
        File file = new File("locale", id + ".json");
        if (!file.exists()) return new JsonObject();

        try (Reader r = new FileReader(file)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        } catch (Exception e) {
            log.warn("Failed to load locale {}", id, e);
            return new JsonObject();
        }
    }

    private void callChangeLangEvent(@NotNull String locale) {
        Registry.instance().callEvent(
                new Event("LanguageChangeEvent").set("locale", locale)
        );
    }

    @Subscribe("PluginLanguageDataEvent")
    public void onPluginLanguageAdd(Event event) {
        JsonObject root = JsonParser.parseString(
                event.get("languageData").asString()
        ).getAsJsonObject();

        JsonObject data = root.has(currentLocale)
                ? root.getAsJsonObject(currentLocale)
                : root.getAsJsonObject("en_us");

        for (String key : data.keySet()) {
            activeTranslations.addProperty(key, data.get(key).getAsString());
        }
    }

    @Subscribe("LanguageChangeEvent")
    public void onLanguageChange(Event e) {
        log.info("Changing language to {}", e.get("locale").asString());
        for (Window w : Window.getWindows()) {
            SwingUtilities.invokeLater(() -> {
                SwingUtilities.updateComponentTreeUI(w);
            });
        }
    }
}