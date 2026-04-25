package me.almana.wgsplicer.core.stats;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.almana.wgsplicer.core.domain.BlockId;

import java.util.HashMap;
import java.util.Map;

public final class DimensionStats {

    public final Map<BlockId, BlockStats> blocks = new HashMap<>();
    public final LongOpenHashSet scannedChunks = new LongOpenHashSet();
    public long chunksScanned;
    public long blocksScanned;

    public boolean isScanned(long chunkPos) {
        return scannedChunks.contains(chunkPos);
    }

    public void markChunkScanned(long chunkPos) {
        scannedChunks.add(chunkPos);
    }

    public BlockStats forBlock(BlockId id) {
        return blocks.computeIfAbsent(id, k -> new BlockStats());
    }

    public void merge(PartialDimensionStats partial) {
        partial.blocks.forEach((id, bs) -> forBlock(id).mergeFrom(bs));
    }
}
