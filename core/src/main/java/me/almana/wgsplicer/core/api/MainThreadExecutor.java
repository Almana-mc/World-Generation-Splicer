package me.almana.wgsplicer.core.api;

@FunctionalInterface
public interface MainThreadExecutor {
    void execute(Runnable task);
}
