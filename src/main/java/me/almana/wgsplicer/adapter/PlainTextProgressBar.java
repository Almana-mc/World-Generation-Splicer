package me.almana.wgsplicer.adapter;

import me.almana.wgsplicer.command.ExportChatBuilder;
import me.almana.wgsplicer.core.api.ProgressBar;
import net.minecraft.commands.CommandSourceStack;

public final class PlainTextProgressBar implements ProgressBar {

    private static final long THROTTLE_MS = 1000L;

    private final CommandSourceStack source;
    private long lastSent;
    private boolean closed;

    public PlainTextProgressBar(CommandSourceStack source) {
        this.source = source;
    }

    @Override
    public void setProgress(float fraction, String title) {
        if (closed) return;
        long now = System.currentTimeMillis();
        if (now - lastSent < THROTTLE_MS) return;
        lastSent = now;
        source.sendSuccess(() -> ExportChatBuilder.info(title), false);
    }

    @Override
    public void close() {
        closed = true;
    }
}
