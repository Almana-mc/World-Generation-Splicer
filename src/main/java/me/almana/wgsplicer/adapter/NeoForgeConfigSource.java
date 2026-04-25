package me.almana.wgsplicer.adapter;

import me.almana.wgsplicer.config.WgSplicerConfig;
import me.almana.wgsplicer.core.api.ConfigSource;
import me.almana.wgsplicer.core.api.ScanMode;
import me.almana.wgsplicer.core.api.ScanTrigger;

import java.util.ArrayList;
import java.util.List;

public final class NeoForgeConfigSource implements ConfigSource {

    @Override public boolean scanEnabled() { return WgSplicerConfig.SCAN_ENABLED.get(); }
    @Override public ScanMode scanMode() { return ScanMode.valueOf(WgSplicerConfig.SCAN_MODE.get().name()); }
    @Override public ScanTrigger scanTrigger() { return ScanTrigger.valueOf(WgSplicerConfig.SCAN_TRIGGER.get().name()); }
    @Override public int maxScansPerTick() { return WgSplicerConfig.MAX_SCANS_PER_TICK.get(); }
    @Override public int maxInFlightWorkers() { return WgSplicerConfig.MAX_IN_FLIGHT_WORKERS.get(); }
    @Override public boolean rescanAlreadyScanned() { return WgSplicerConfig.RESCAN_ALREADY_SCANNED.get(); }
    @Override public int maxManualRadius() { return WgSplicerConfig.MAX_MANUAL_RADIUS.get(); }
    @Override public boolean includeBiomeStats() { return WgSplicerConfig.INCLUDE_BIOME_STATS.get(); }
    @Override public boolean includeYLevelStats() { return WgSplicerConfig.INCLUDE_Y_LEVEL_STATS.get(); }
    @Override public int yLevelBucketSize() { return WgSplicerConfig.Y_LEVEL_BUCKET_SIZE.get(); }
    @Override public List<String> extraBlockIds() { return new ArrayList<>(WgSplicerConfig.EXTRA_BLOCK_IDS.get()); }
    @Override public List<String> extraNamespaces() { return new ArrayList<>(WgSplicerConfig.EXTRA_NAMESPACES.get()); }
    @Override public List<String> dimensionBlacklist() { return new ArrayList<>(WgSplicerConfig.SCAN_DIMENSION_BLACKLIST.get()); }

    @Override public boolean exportEnabled() { return WgSplicerConfig.EXPORT_ENABLED.get(); }
    @Override public String exportFolder() { return WgSplicerConfig.EXPORT_FOLDER.get(); }
    @Override public boolean exportAsync() { return WgSplicerConfig.EXPORT_ASYNC.get(); }
    @Override public String exportJsonFilenamePattern() { return WgSplicerConfig.EXPORT_JSON_FILENAME_PATTERN.get(); }
    @Override public boolean exportJsonPretty() { return WgSplicerConfig.EXPORT_JSON_PRETTY.get(); }
    @Override public String websiteUrl() { return WgSplicerConfig.WEBSITE_URL.get(); }

    @Override public int permissionLevel() { return WgSplicerConfig.PERMISSION_LEVEL.get(); }
    @Override public boolean resetRequiresOp() { return WgSplicerConfig.RESET_REQUIRES_OP.get(); }
    @Override public boolean progressBarEnabled() { return WgSplicerConfig.PROGRESS_BAR_ENABLED.get(); }
}
