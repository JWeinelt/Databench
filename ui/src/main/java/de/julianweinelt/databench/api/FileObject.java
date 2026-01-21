package de.julianweinelt.databench.api;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record FileObject(String path, String name, boolean directory, List<FileObject> children, FileType type) {
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileObject o) {
            return o.path.equals(path) && o.name.equals(name) && o.directory == directory &&
                    o.children.equals(children) && o.type.equals(type);
        }
        return false;
    }
    public String json() {
        return new Gson().toJson(this);
    }

    @Override
    public @NotNull String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return json().hashCode();
    }
}