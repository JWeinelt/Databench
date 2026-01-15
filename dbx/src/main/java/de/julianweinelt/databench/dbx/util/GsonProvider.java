package de.julianweinelt.databench.dbx.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class GsonProvider {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    private GsonProvider() {}

    public static Gson gson() {
        return GSON;
    }
}
