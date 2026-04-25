package me.almana.wgsplicer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.almana.wgsplicer.adapter.ComponentTextSink;
import me.almana.wgsplicer.adapter.WgSplicerMod;
import me.almana.wgsplicer.config.WgSplicerConfig;
import me.almana.wgsplicer.core.api.TextSink;
import me.almana.wgsplicer.core.domain.ChunkCoord;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.domain.ExportContext;
import me.almana.wgsplicer.core.service.WgSplicerService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class WgSplicerCommand {

    private WgSplicerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        int baseLevel = WgSplicerConfig.PERMISSION_LEVEL.get();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("wgsplicer")
                .requires(s -> s.hasPermission(baseLevel))
                .then(Commands.literal("status").executes(WgSplicerCommand::status))
                .then(Commands.literal("export").executes(WgSplicerCommand::export))
                .then(Commands.literal("reset")
                        .requires(s -> s.hasPermission(WgSplicerConfig.RESET_REQUIRES_OP.get() ? 4 : baseLevel))
                        .executes(WgSplicerCommand::reset))
                .then(Commands.literal("scanchunk").executes(WgSplicerCommand::scanChunk))
                .then(Commands.literal("scanradius")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, 1000))
                                .executes(ctx -> scanRadius(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("scanall")
                        .executes(ctx -> scanAll(ctx, 200))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1000))
                                .executes(ctx -> scanAll(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("skipcurrent").executes(WgSplicerCommand::skipCurrent));
        dispatcher.register(root);
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        WgSplicerService svc = WgSplicerMod.service();
        TextSink sink = new ComponentTextSink(ctx.getSource());
        if (svc == null) { sink.failure("World Generation Splicer not initialized"); return 0; }
        svc.status(sink);
        return 1;
    }

    private static int export(CommandContext<CommandSourceStack> ctx) {
        WgSplicerService svc = WgSplicerMod.service();
        TextSink sink = new ComponentTextSink(ctx.getSource());
        if (svc == null) { sink.failure("World Generation Splicer not initialized"); return 0; }
        svc.export(buildExportContext(), sink);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        WgSplicerService svc = WgSplicerMod.service();
        TextSink sink = new ComponentTextSink(ctx.getSource());
        if (svc == null) { sink.failure("World Generation Splicer not initialized"); return 0; }
        svc.reset(sink);
        return 1;
    }

    private static int skipCurrent(CommandContext<CommandSourceStack> ctx) {
        WgSplicerService svc = WgSplicerMod.service();
        TextSink sink = new ComponentTextSink(ctx.getSource());
        if (svc == null) { sink.failure("World Generation Splicer not initialized"); return 0; }
        svc.skipCurrent(sink);
        return 1;
    }

    private static int scanChunk(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        WgSplicerService svc = WgSplicerMod.service();
        TextSink sink = new ComponentTextSink(ctx.getSource());
        if (svc == null) { sink.failure("World Generation Splicer not initialized"); return 0; }
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        ChunkPos pos = new ChunkPos(player.blockPosition());
        DimensionId dim = toDim(level);
        svc.scanChunk(dim, new ChunkCoord(pos.x, pos.z), sink);
        return 1;
    }

    private static int scanRadius(CommandContext<CommandSourceStack> ctx, int radius) throws CommandSyntaxException {
        WgSplicerService svc = WgSplicerMod.service();
        TextSink sink = new ComponentTextSink(ctx.getSource());
        if (svc == null) { sink.failure("World Generation Splicer not initialized"); return 0; }
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        ChunkPos center = new ChunkPos(player.blockPosition());
        DimensionId dim = toDim(level);
        svc.scanRadius(dim, new ChunkCoord(center.x, center.z), radius, sink);
        return 1;
    }

    private static int scanAll(CommandContext<CommandSourceStack> ctx, int radius) {
        WgSplicerService svc = WgSplicerMod.service();
        TextSink sink = new ComponentTextSink(ctx.getSource());
        if (svc == null) { sink.failure("World Generation Splicer not initialized"); return 0; }
        svc.scanAll(radius, buildExportContext(), sink);
        return 1;
    }

    private static DimensionId toDim(ServerLevel level) {
        ResourceLocation rl = level.dimension().location();
        return new DimensionId(rl.getNamespace(), rl.getPath());
    }

    private static ExportContext buildExportContext() {
        return new ExportContext(
                WgSplicerMod.exportMcVersion(),
                WgSplicerMod.exportLoaderName(),
                WgSplicerMod.exportModVersion());
    }
}
