package me.almana.wgsplicer.core.domain;

import java.util.Objects;

public record DimensionId(String namespace, String path) {

    public DimensionId {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(path);
    }

    public static DimensionId of(String namespace, String path) {
        return new DimensionId(namespace, path);
    }

    public static DimensionId parse(String s) {
        int colon = s.indexOf(':');
        if (colon <= 0 || colon == s.length() - 1) {
            throw new IllegalArgumentException("Invalid id: " + s);
        }
        return new DimensionId(s.substring(0, colon), s.substring(colon + 1));
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
