package me.almana.wgsplicer.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class WgSplicerConfig {

    public enum ScanMode { ORES_ONLY, CONFIGURED_ONLY, ORES_AND_CONFIGURED, ALL_BLOCKS_DEBUG }
    public enum ScanTrigger { NEW_CHUNKS, ALL_LOADS, MANUAL_ONLY }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue SCAN_ENABLED;
    public static final ModConfigSpec.EnumValue<ScanMode> SCAN_MODE;
    public static final ModConfigSpec.EnumValue<ScanTrigger> SCAN_TRIGGER;
    public static final ModConfigSpec.IntValue MAX_SCANS_PER_TICK;
    public static final ModConfigSpec.IntValue MAX_IN_FLIGHT_WORKERS;
    public static final ModConfigSpec.BooleanValue RESCAN_ALREADY_SCANNED;
    public static final ModConfigSpec.IntValue MAX_MANUAL_RADIUS;
    public static final ModConfigSpec.BooleanValue INCLUDE_BIOME_STATS;
    public static final ModConfigSpec.BooleanValue INCLUDE_Y_LEVEL_STATS;
    public static final ModConfigSpec.IntValue Y_LEVEL_BUCKET_SIZE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_BLOCK_IDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_NAMESPACES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SCAN_DIMENSION_BLACKLIST;

    public static final ModConfigSpec.BooleanValue EXPORT_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> EXPORT_FOLDER;
    public static final ModConfigSpec.BooleanValue EXPORT_ASYNC;
    public static final ModConfigSpec.ConfigValue<String> EXPORT_JSON_FILENAME_PATTERN;
    public static final ModConfigSpec.BooleanValue EXPORT_JSON_PRETTY;
    public static final ModConfigSpec.ConfigValue<String> WEBSITE_URL;

    public static final ModConfigSpec.IntValue PERMISSION_LEVEL;
    public static final ModConfigSpec.BooleanValue RESET_REQUIRES_OP;
    public static final ModConfigSpec.BooleanValue PROGRESS_BAR_ENABLED;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("scan");
        SCAN_ENABLED = b.comment("Whether chunk scanning is enabled").define("enabled", true);
        SCAN_MODE = b.comment("ORES_ONLY | CONFIGURED_ONLY | ORES_AND_CONFIGURED | ALL_BLOCKS_DEBUG")
                .defineEnum("mode", ScanMode.ORES_AND_CONFIGURED);
        SCAN_TRIGGER = b.comment("ALL_LOADS (default, backfills existing worlds) | NEW_CHUNKS | MANUAL_ONLY")
                .defineEnum("trigger", ScanTrigger.ALL_LOADS);
        MAX_SCANS_PER_TICK = b.comment("Max chunk-scan tasks drained per server tick")
                .defineInRange("maxScansPerTick", 2, 1, 64);
        MAX_IN_FLIGHT_WORKERS = b.comment("Cap on pending worker jobs; tick drains pause when exceeded")
                .defineInRange("maxInFlightWorkers", 8, 1, 64);
        RESCAN_ALREADY_SCANNED = b.comment("Rescan chunks that have already been scanned")
                .define("rescanAlreadyScanned", false);
        MAX_MANUAL_RADIUS = b.comment("Max radius (in chunks) for /wgsplicer scanradius")
                .defineInRange("maxManualRadius", 100, 1, 1000);
        INCLUDE_BIOME_STATS = b.comment("Record per-biome block counts")
                .define("includeBiomeStats", true);
        INCLUDE_Y_LEVEL_STATS = b.comment("Record Y-level bucket counts per block")
                .define("includeYLevelStats", true);
        Y_LEVEL_BUCKET_SIZE = b.comment("Y-bucket size for Y-level stats (1 = per-Y, 16 = per-section)")
                .defineInRange("yLevelBucketSize", 1, 1, 64);
        EXTRA_BLOCK_IDS = b.comment("Extra block IDs to track, e.g. [\"modid:some_ore\"]")
                .defineListAllowEmpty("extraBlockIds", List.of(), () -> "", WgSplicerConfig::isValidResourceLocation);
        EXTRA_NAMESPACES = b.comment("Track all blocks whose namespace matches, e.g. [\"create\"]")
                .defineListAllowEmpty("extraNamespaces", List.of(), () -> "", WgSplicerConfig::isValidNamespace);
        SCAN_DIMENSION_BLACKLIST = b.comment("Dimensions to skip in /wgsplicer scanall, e.g. [\"minecraft:the_end\"]")
                .defineListAllowEmpty("dimensionBlacklist", List.of(), () -> "", WgSplicerConfig::isValidResourceLocation);
        b.pop();

        b.push("export");
        EXPORT_ENABLED = b.comment("Whether /wgsplicer export is enabled")
                .define("enabled", true);
        EXPORT_FOLDER = b.comment("Export folder (relative to game directory)")
                .define("folder", "wgsplicer_exports");
        EXPORT_ASYNC = b.comment("Run export on worker thread")
                .define("async", true);
        EXPORT_JSON_FILENAME_PATTERN = b.comment("JSON filename pattern; %s is replaced by timestamp")
                .define("jsonFilenamePattern", "wgsplicer-stats-%s.json");
        EXPORT_JSON_PRETTY = b.comment("Pretty-print JSON (larger file, easier to read)")
                .define("jsonPretty", false);
        WEBSITE_URL = b.comment("Web viewer URL shown in chat after export")
                .define("websiteUrl", "https://almanax-21.github.io/WorldGenerationSplicer/");
        b.pop();

        b.push("commands");
        PERMISSION_LEVEL = b.comment("Permission level required for /wgsplicer")
                .defineInRange("permissionLevel", 2, 0, 4);
        RESET_REQUIRES_OP = b.comment("Require op (permission 4) for /wgsplicer reset")
                .define("resetRequiresOp", true);
        PROGRESS_BAR_ENABLED = b.comment("Show a boss-bar progress indicator for /wgsplicer scanradius and scanall")
                .define("progressBarEnabled", true);
        b.pop();

        SPEC = b.build();
    }

    private WgSplicerConfig() {}

    private static boolean isValidResourceLocation(Object o) {
        if (!(o instanceof String s) || s.isEmpty()) return false;
        int colon = s.indexOf(':');
        if (colon <= 0 || colon == s.length() - 1) return false;
        return isValidNamespace(s.substring(0, colon)) && isValidPath(s.substring(colon + 1));
    }

    private static boolean isValidNamespace(Object o) {
        if (!(o instanceof String s) || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c == '_' || c == '-' || c == '.' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))) return false;
        }
        return true;
    }

    private static boolean isValidPath(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c == '_' || c == '-' || c == '.' || c == '/' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))) return false;
        }
        return true;
    }
}
