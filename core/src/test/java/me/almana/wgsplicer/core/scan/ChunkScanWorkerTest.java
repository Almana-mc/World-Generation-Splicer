package me.almana.wgsplicer.core.scan;

import me.almana.wgsplicer.core.api.MainThreadExecutor;
import me.almana.wgsplicer.core.api.StatsRepository;
import me.almana.wgsplicer.core.domain.BiomeId;
import me.almana.wgsplicer.core.domain.BlockId;
import me.almana.wgsplicer.core.domain.BlockMatcher;
import me.almana.wgsplicer.core.domain.BlockPalette;
import me.almana.wgsplicer.core.domain.ChunkCoord;
import me.almana.wgsplicer.core.domain.ChunkSnapshot;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.domain.SectionSnapshot;
import me.almana.wgsplicer.core.stats.BlockStats;
import me.almana.wgsplicer.core.stats.DimensionStats;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkScanWorkerTest {

    private static final DimensionId DIM = new DimensionId("minecraft", "overworld");
    private static final BlockId DIAMOND = new BlockId("minecraft", "diamond_ore");

    @Test
    void scanRecordsMatchingBlocksWithBiomeAndY() {
        BlockPalette palette = (matcher, visitor) -> {
            visitor.visit(0, 0, 0, DIAMOND);
            visitor.visit(1, 5, 2, DIAMOND);
        };
        BiomeId[] biomes = new BiomeId[64];
        biomes[0] = new BiomeId("minecraft", "plains");
        SectionSnapshot section = new SectionSnapshot(64, palette, biomes);
        ChunkSnapshot snap = new ChunkSnapshot(DIM, new ChunkCoord(0, 0), List.of(section), 1);

        BlockMatcher matcher = id -> true;
        SimpleRepo repo = new SimpleRepo();
        MainThreadExecutor inline = Runnable::run;

        ChunkScanWorker.scan(snap, matcher, 1, true, inline, repo);

        DimensionStats ds = repo.forDim(DIM);
        BlockStats bs = ds.blocks.get(DIAMOND);
        assertNotNull(bs);
        assertEquals(2L, bs.totalCount);
        assertEquals(64, bs.minY);
        assertEquals(69, bs.maxY);
        assertEquals(1L, ds.chunksScanned);
        assertEquals(4096L, ds.blocksScanned);
        assertTrue(bs.biomeCounts.size() >= 1);
    }

    @Test
    void scanFiltersByMatcher() {
        BlockId iron = new BlockId("minecraft", "iron_ore");
        BlockPalette palette = (matcher, visitor) -> {
            if (matcher.matches(DIAMOND)) visitor.visit(0, 0, 0, DIAMOND);
            if (matcher.matches(iron)) visitor.visit(0, 1, 0, iron);
        };
        SectionSnapshot section = new SectionSnapshot(0, palette, null);
        ChunkSnapshot snap = new ChunkSnapshot(DIM, new ChunkCoord(0, 0), List.of(section), 1);

        BlockMatcher matcher = id -> id.equals(DIAMOND);
        SimpleRepo repo = new SimpleRepo();
        ChunkScanWorker.scan(snap, matcher, 0, false, Runnable::run, repo);

        DimensionStats ds = repo.forDim(DIM);
        assertEquals(1, ds.blocks.size());
        assertNotNull(ds.blocks.get(DIAMOND));
    }

    private static final class SimpleRepo implements StatsRepository {
        private final Map<DimensionId, DimensionStats> dims = new HashMap<>();
        boolean dirty;

        @Override public DimensionStats forDim(DimensionId d) { return dims.computeIfAbsent(d, k -> new DimensionStats()); }
        @Override public Map<DimensionId, DimensionStats> all() { return dims; }
        @Override public void drop(DimensionId d) { dims.remove(d); }
        @Override public void clear() { dims.clear(); }
        @Override public void markDirty() { dirty = true; }
        @Override public boolean ready() { return true; }
    }
}
