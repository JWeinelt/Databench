package de.julianweinelt.databench.ui.flow;

import java.util.List;
import java.util.Map;

public record HttpResponse<T>(int statusCode, Map<String, List<String>> headers, T body) {

    public boolean is2xx() {
        return statusCode >= 200 && statusCode < 300;
    }
}
