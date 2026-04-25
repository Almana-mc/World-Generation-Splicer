package me.almana.wgsplicer.core.stats;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.almana.wgsplicer.core.api.WorldReader;
import me.almana.wgsplicer.core.domain.BiomeId;
import me.almana.wgsplicer.core.domain.BlockId;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.domain.YBounds;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StatsSnapshot {
    public final Map<DimensionId, DimSnapshot> dims = new LinkedHashMap<>();

    public static final class DimSnapshot {
        public final Map<BlockId, BlockSnapshot> blocks = new HashMap<>();
        public long chunksScanned;
        public long blocksScanned;
        public int minY;
        public int maxY;
        public boolean hasYBounds;
    }

    public static final class BlockSnapshot {
        public long totalCount;
        public int minY;
        public int maxY;
        public long firstSeenChunk;
        public long lastSeenChunk;
        public final Object2LongOpenHashMap<BiomeId> biomeCounts = new Object2LongOpenHashMap<>();
        public final Int2LongOpenHashMap yBuckets = new Int2LongOpenHashMap();
    }

    public static StatsSnapshot of(Map<DimensionId, DimensionStats> src) {
        StatsSnapshot out = new StatsSnapshot();
        src.forEach((key, ds) -> {
            DimSnapshot d = new DimSnapshot();
            d.chunksScanned = ds.chunksScanned;
            d.blocksScanned = ds.blocksScanned;
            ds.blocks.forEach((id, bs) -> {
                BlockSnapshot b = new BlockSnapshot();
                b.totalCount = bs.totalCount;
                b.minY = bs.minY;
                b.maxY = bs.maxY;
                b.firstSeenChunk = bs.firstSeenChunk;
                b.lastSeenChunk = bs.lastSeenChunk;
                for (Object2LongMap.Entry<BiomeId> e : bs.biomeCounts.object2LongEntrySet()) {
                    b.biomeCounts.put(e.getKey(), e.getLongValue());
                }
                for (Int2LongMap.Entry e : bs.yBuckets.int2LongEntrySet()) {
                    b.yBuckets.put(e.getIntKey(), e.getLongValue());
                }
                d.blocks.put(id, b);
            });
            out.dims.put(key, d);
        });
        return out;
    }

    public static StatsSnapshot of(Map<DimensionId, DimensionStats> src, WorldReader world) {
        StatsSnapshot out = of(src);
        out.dims.forEach((key, dim) -> {
            YBounds bounds = world.yBounds(key);
            if (bounds != null) {
                dim.minY = bounds.minY();
                dim.maxY = bounds.maxY();
                dim.hasYBounds = true;
            }
        });
        return out;
    }
}
