package me.almana.wgsplicer.core.api;

import java.nio.file.Path;

@FunctionalInterface
public interface ServerPaths {
    Path serverDirectory();
}
