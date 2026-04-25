package me.almana.wgsplicer.core.domain;

import java.util.Objects;

public record BiomeId(String namespace, String path) {

    public BiomeId {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(path);
    }

    public static BiomeId of(String namespace, String path) {
        return new BiomeId(namespace, path);
    }

    public static BiomeId parse(String s) {
        int colon = s.indexOf(':');
        if (colon <= 0 || colon == s.length() - 1) {
            throw new IllegalArgumentException("Invalid id: " + s);
        }
        return new BiomeId(s.substring(0, colon), s.substring(colon + 1));
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
