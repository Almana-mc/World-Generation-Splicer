package me.almana.wgsplicer.core.stats;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.almana.wgsplicer.core.domain.BiomeId;

public final class BlockStats {
    public long totalCount;
    public int minY = Integer.MAX_VALUE;
    public int maxY = Integer.MIN_VALUE;
    public long firstSeenChunk = Long.MIN_VALUE;
    public long lastSeenChunk = Long.MIN_VALUE;
    public final Object2LongOpenHashMap<BiomeId> biomeCounts = new Object2LongOpenHashMap<>();
    public final Int2LongOpenHashMap yBuckets = new Int2LongOpenHashMap();

    public void record(int y, long chunkPos, BiomeId biomeId, int yBucketSize) {
        totalCount++;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
        if (firstSeenChunk == Long.MIN_VALUE) firstSeenChunk = chunkPos;
        lastSeenChunk = chunkPos;
        if (biomeId != null) biomeCounts.addTo(biomeId, 1L);
        if (yBucketSize > 0) yBuckets.addTo(Math.floorDiv(y, yBucketSize), 1L);
    }

    public void mergeFrom(BlockStats other) {
        totalCount += other.totalCount;
        if (other.minY < minY) minY = other.minY;
        if (other.maxY > maxY) maxY = other.maxY;
        if (firstSeenChunk == Long.MIN_VALUE) firstSeenChunk = other.firstSeenChunk;
        if (other.lastSeenChunk != Long.MIN_VALUE) lastSeenChunk = other.lastSeenChunk;
        other.biomeCounts.forEach((k, v) -> biomeCounts.addTo(k, v));
        other.yBuckets.int2LongEntrySet().forEach(e -> yBuckets.addTo(e.getIntKey(), e.getLongValue()));
    }
}
