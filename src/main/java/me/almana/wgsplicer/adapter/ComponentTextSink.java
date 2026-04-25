package me.almana.wgsplicer.adapter;

import me.almana.wgsplicer.command.ExportChatBuilder;
import me.almana.wgsplicer.config.WgSplicerConfig;
import me.almana.wgsplicer.core.api.ProgressBar;
import me.almana.wgsplicer.core.api.TextSink;
import me.almana.wgsplicer.core.domain.ExportResultMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class ComponentTextSink implements TextSink {

    private final CommandSourceStack source;

    public ComponentTextSink(CommandSourceStack source) {
        this.source = source;
    }

    @Override
    public void info(String msg) {
        source.sendSuccess(() -> ExportChatBuilder.info(msg), false);
    }

    @Override
    public void failure(String msg) {
        source.sendFailure(ExportChatBuilder.failure(msg));
    }

    @Override
    public void success(ExportResultMessage msg) {
        MinecraftServer server = source.getServer();
        source.sendSuccess(() -> ExportChatBuilder.success(server, msg.rows(), msg.json(), msg.websiteUrl()), false);
    }

    @Override
    public ProgressBar createProgressBar(String label) {
        MinecraftServer server = source.getServer();
        ServerPlayer player = source.getPlayer();
        UUID playerId = player == null ? null : player.getUUID();
        if (playerId != null && WgSplicerConfig.PROGRESS_BAR_ENABLED.get()) {
            return new NeoForgeProgressBar(server, playerId, label);
        }
        return new PlainTextProgressBar(source);
    }
}
