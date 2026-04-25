package me.almana.wgsplicer.core.export;

import me.almana.wgsplicer.core.api.ConfigSource;
import me.almana.wgsplicer.core.api.ScanMode;
import me.almana.wgsplicer.core.api.ScanTrigger;
import me.almana.wgsplicer.core.domain.BlockId;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.domain.ExportContext;
import me.almana.wgsplicer.core.stats.StatsSnapshot;
import me.almana.wgsplicer.core.stats.StatsSnapshot.BlockSnapshot;
import me.almana.wgsplicer.core.stats.StatsSnapshot.DimSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonStatsExporterTest {

    @Test
    void writesFileWithMetaAndDimensions(@TempDir Path tmp) throws Exception {
        StatsSnapshot snap = new StatsSnapshot();
        DimSnapshot dim = new DimSnapshot();
        dim.minY = -64;
        dim.maxY = 320;
        dim.hasYBounds = true;
        dim.chunksScanned = 100;
        dim.blocksScanned = 409600;

        BlockSnapshot bs = new BlockSnapshot();
        bs.totalCount = 50;
        bs.minY = 0;
        bs.maxY = 16;
        for (int y = 0; y <= 16; y++) bs.yBuckets.put(y, 3L);
        bs.totalCount = 17 * 3;
        dim.blocks.put(new BlockId("minecraft", "diamond_ore"), bs);

        snap.dims.put(new DimensionId("minecraft", "overworld"), dim);

        ConfigSource cfg = new TestConfig();
        ExportContext ctx = new ExportContext("1.21.1", "Test", "1.0.0");
        JsonStatsExporter exporter = new JsonStatsExporter(cfg);

        StatsExporter.Result result = exporter.export(snap, tmp, "out.json", ctx);
        assertTrue(Files.exists(result.file()));
        String content = Files.readString(result.file());
        assertTrue(content.contains("\"mcVersion\""));
        assertTrue(content.contains("\"diamond_ore\"") || content.contains("\"minecraft:diamond_ore\""));
        assertEquals(1, result.rows());
    }

    private static final class TestConfig implements ConfigSource {
        @Override public boolean scanEnabled() { return true; }
        @Override public ScanMode scanMode() { return ScanMode.ORES_AND_CONFIGURED; }
        @Override public ScanTrigger scanTrigger() { return ScanTrigger.ALL_LOADS; }
        @Override public int maxScansPerTick() { return 2; }
        @Override public int maxInFlightWorkers() { return 8; }
        @Override public boolean rescanAlreadyScanned() { return false; }
        @Override public int maxManualRadius() { return 100; }
        @Override public boolean includeBiomeStats() { return true; }
        @Override public boolean includeYLevelStats() { return true; }
        @Override public int yLevelBucketSize() { return 1; }
        @Override public List<String> extraBlockIds() { return List.of(); }
        @Override public List<String> extraNamespaces() { return List.of(); }
        @Override public List<String> dimensionBlacklist() { return List.of(); }
        @Override public boolean exportEnabled() { return true; }
        @Override public String exportFolder() { return "exports"; }
        @Override public boolean exportAsync() { return false; }
        @Override public String exportJsonFilenamePattern() { return "stats-%s.json"; }
        @Override public boolean exportJsonPretty() { return false; }
        @Override public String websiteUrl() { return ""; }
        @Override public int permissionLevel() { return 2; }
        @Override public boolean resetRequiresOp() { return true; }
        @Override public boolean progressBarEnabled() { return true; }
    }
}
