package me.almana.wgsplicer.adapter;

import com.mojang.logging.LogUtils;
import me.almana.wgsplicer.WgSplicer;
import me.almana.wgsplicer.core.api.ConfigSource;
import me.almana.wgsplicer.core.api.MainThreadExecutor;
import me.almana.wgsplicer.core.api.ServerPaths;
import me.almana.wgsplicer.core.scan.ScanQueue;
import me.almana.wgsplicer.core.service.WgSplicerService;
import me.almana.wgsplicer.persistence.WgSplicerSavedData;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

public final class WgSplicerMod {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static WgSplicerService SERVICE;
    private static ScanQueue QUEUE;
    private static WgSplicerSavedData DATA;
    private static MinecraftServer SERVER;

    private WgSplicerMod() {}

    public static void bootstrap(MinecraftServer server) {
        SERVER = server;
        ServerLevel overworld = server.overworld();
        DATA = overworld.getDataStorage().computeIfAbsent(WgSplicerSavedData.factory(), WgSplicerSavedData.DATA_ID);

        ConfigSource cfg = new NeoForgeConfigSource();
        NeoForgeWorldReader world = new NeoForgeWorldReader(server, cfg);
        NeoForgeBlockRegistryView registry = new NeoForgeBlockRegistryView();
        MainThreadExecutor mt = server::execute;
        ServerPaths paths = server::getServerDirectory;

        QUEUE = new ScanQueue(cfg, world, mt, DATA);
        QUEUE.start();

        SERVICE = new WgSplicerService(DATA, QUEUE, world, cfg, paths, mt, registry);
        SERVICE.rebuildMatcher();

        LOGGER.info("[WGSplicer] Bootstrap complete, ready={}, mode={}, trigger={}",
                DATA.ready(), cfg.scanMode(), cfg.scanTrigger());
    }

    public static void shutdown() {
        if (QUEUE != null) QUEUE.stop();
        SERVICE = null;
        QUEUE = null;
        DATA = null;
        SERVER = null;
    }

    public static WgSplicerService service() { return SERVICE; }
    public static ScanQueue queue() { return QUEUE; }
    public static WgSplicerSavedData data() { return DATA; }
    public static MinecraftServer server() { return SERVER; }

    public static boolean ready() { return SERVICE != null; }

    public static String exportLoaderName() { return "NeoForge"; }

    public static String exportMcVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    public static String exportModVersion() {
        return ModList.get().getModContainerById(WgSplicer.MODID)
                .map(mc -> mc.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
}
