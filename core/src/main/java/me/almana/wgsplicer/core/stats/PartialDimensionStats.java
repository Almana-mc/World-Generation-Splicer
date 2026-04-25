package me.almana.wgsplicer.core.stats;

import me.almana.wgsplicer.core.domain.BiomeId;
import me.almana.wgsplicer.core.domain.BlockId;

import java.util.HashMap;
import java.util.Map;

public final class PartialDimensionStats {
    public final Map<BlockId, BlockStats> blocks = new HashMap<>();

    public void record(BlockId id, int y, long chunkPos, BiomeId biomeId, int yBucketSize) {
        blocks.computeIfAbsent(id, k -> new BlockStats()).record(y, chunkPos, biomeId, yBucketSize);
    }
}
