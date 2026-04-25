package me.almana.wgsplicer.event;

import com.mojang.logging.LogUtils;
import me.almana.wgsplicer.adapter.WgSplicerMod;
import me.almana.wgsplicer.command.WgSplicerCommand;
import me.almana.wgsplicer.config.WgSplicerConfig;
import me.almana.wgsplicer.config.WgSplicerConfig.ScanTrigger;
import me.almana.wgsplicer.core.domain.ChunkCoord;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.service.WgSplicerService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public final class ModEvents {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger CHUNK_LOAD_COUNT = new AtomicInteger();

    private ModEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("[WGSplicer] ServerStartedEvent fired, bootstrapping");
        MinecraftServer server = event.getServer();
        WgSplicerMod.bootstrap(server);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        WgSplicerMod.shutdown();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!WgSplicerConfig.SCAN_ENABLED.get()) return;
        if (!WgSplicerMod.ready()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        ScanTrigger trigger = WgSplicerConfig.SCAN_TRIGGER.get();
        if (trigger == ScanTrigger.MANUAL_ONLY) return;
        if (trigger == ScanTrigger.NEW_CHUNKS && !event.isNewChunk()) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        int n = CHUNK_LOAD_COUNT.incrementAndGet();
        if (n <= 5 || n % 32 == 0) {
            LOGGER.info("[WGSplicer] ChunkEvent.Load #{} at {} in {} (newChunk={})",
                    n, chunk.getPos(), serverLevel.dimension().location(), event.isNewChunk());
        }
        ResourceLocation rl = serverLevel.dimension().location();
        DimensionId dim = new DimensionId(rl.getNamespace(), rl.getPath());
        WgSplicerMod.queue().enqueue(dim, new ChunkCoord(chunk.getPos().x, chunk.getPos().z));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!WgSplicerMod.ready()) return;
        WgSplicerMod.service().tick();
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;
        WgSplicerService svc = WgSplicerMod.service();
        if (svc != null) svc.rebuildMatcher();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        WgSplicerCommand.register(event.getDispatcher());
    }
}
