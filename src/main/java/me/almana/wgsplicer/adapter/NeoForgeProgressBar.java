package me.almana.wgsplicer.adapter;

import me.almana.wgsplicer.core.api.ProgressBar;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.UUID;

public final class NeoForgeProgressBar implements ProgressBar {

    private final MinecraftServer server;
    private final UUID playerId;
    private final ServerBossEvent bar;
    private boolean closed;

    public NeoForgeProgressBar(MinecraftServer server, UUID playerId, String label) {
        this.server = server;
        this.playerId = playerId;
        this.bar = new ServerBossEvent(Component.literal(label),
                BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
        attachPlayer();
    }

    private void attachPlayer() {
        if (playerId == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(playerId);
        if (p != null) bar.addPlayer(p);
    }

    @Override
    public void setProgress(float fraction, String title) {
        if (closed) return;
        attachPlayer();
        bar.setProgress(Math.max(0f, Math.min(1f, fraction)));
        bar.setName(Component.literal(title));
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        bar.setProgress(1f);
        bar.removeAllPlayers();
        bar.setVisible(false);
    }
}
