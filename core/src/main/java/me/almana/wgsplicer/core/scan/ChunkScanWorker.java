package me.almana.wgsplicer.core.scan;

import me.almana.wgsplicer.core.api.MainThreadExecutor;
import me.almana.wgsplicer.core.api.StatsRepository;
import me.almana.wgsplicer.core.domain.BiomeId;
import me.almana.wgsplicer.core.domain.BlockMatcher;
import me.almana.wgsplicer.core.domain.ChunkSnapshot;
import me.almana.wgsplicer.core.domain.SectionSnapshot;
import me.almana.wgsplicer.core.stats.DimensionStats;
import me.almana.wgsplicer.core.stats.PartialDimensionStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChunkScanWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkScanWorker.class);

    private ChunkScanWorker() {}

    public static void scan(ChunkSnapshot snap,
                            BlockMatcher matcher,
                            int yBucketSize,
                            boolean biomeEnabled,
                            MainThreadExecutor mainThread,
                            StatsRepository repo) {
        long posLong = snap.pos.packed();
        PartialDimensionStats partial = new PartialDimensionStats();

        for (SectionSnapshot s : snap.sections) {
            BiomeId[] biomes = s.biomes;
            int baseY = s.baseY;
            s.palette.forEachMatching(matcher, (lx, ly, lz, id) -> {
                int y = baseY + ly;
                BiomeId biomeId = (biomeEnabled && biomes != null)
                        ? biomes[((ly >> 2) << 4) | ((lz >> 2) << 2) | (lx >> 2)]
                        : null;
                partial.record(id, y, posLong, biomeId, yBucketSize);
            });
        }

        int sectionsVisited = snap.sections.size();
        long recorded = partial.blocks.values().stream().mapToLong(b -> b.totalCount).sum();
        LOGGER.info("[WGSplicer] Worker {} @ {},{}: {} sections, {} blocks recorded across {} types",
                snap.dim, snap.pos.x(), snap.pos.z(), sectionsVisited, recorded, partial.blocks.size());

        mainThread.execute(() -> {
            if (!repo.ready()) return;
            DimensionStats ds = repo.forDim(snap.dim);
            ds.merge(partial);
            ds.blocksScanned += 4096L * sectionsVisited;
            ds.markChunkScanned(posLong);
            ds.chunksScanned += 1;
            repo.markDirty();
        });
    }
}
