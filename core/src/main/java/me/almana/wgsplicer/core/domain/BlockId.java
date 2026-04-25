package me.almana.wgsplicer.core.domain;

import java.util.Objects;

public record BlockId(String namespace, String path) {

    public BlockId {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(path);
    }

    public static BlockId of(String namespace, String path) {
        return new BlockId(namespace, path);
    }

    public static BlockId parse(String s) {
        int colon = s.indexOf(':');
        if (colon <= 0 || colon == s.length() - 1) {
            throw new IllegalArgumentException("Invalid id: " + s);
        }
        return new BlockId(s.substring(0, colon), s.substring(colon + 1));
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
