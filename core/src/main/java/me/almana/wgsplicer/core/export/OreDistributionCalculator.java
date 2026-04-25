package me.almana.wgsplicer.core.export;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import me.almana.wgsplicer.core.domain.BlockId;
import me.almana.wgsplicer.core.stats.StatsSnapshot.BlockSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class OreDistributionCalculator {

    private OreDistributionCalculator() {}

    public static List<OreDistribution> calculate(List<Map.Entry<BlockId, BlockSnapshot>> ores,
                                                  int bucketSize, int minY, int maxY) {
        List<OreDistribution> out = new ArrayList<>();
        int span = maxY - minY + 1;
        if (span <= 0) return out;

        for (Map.Entry<BlockId, BlockSnapshot> e : ores) {
            TreeMap<Integer, Long> clamped = new TreeMap<>();
            for (Int2LongMap.Entry bucket : e.getValue().yBuckets.int2LongEntrySet()) {
                long count = bucket.getLongValue();
                if (count <= 0) continue;
                int y = bucket.getIntKey() * bucketSize;
                int clampedY = clamp(y, minY, maxY);
                clamped.merge(clampedY, count, Long::sum);
            }
            if (clamped.isEmpty()) continue;

            long[] counts = new long[span];
            long total = 0L;
            for (Map.Entry<Integer, Long> point : clamped.entrySet()) {
                long count = point.getValue();
                counts[point.getKey() - minY] = count;
                total += count;
            }
            if (total <= 0L) continue;

            double[] percentages = new double[span];
            double maxPercentage = 0.0;
            for (int i = 0; i < span; i++) {
                percentages[i] = (double) counts[i] / total;
                if (percentages[i] > maxPercentage) maxPercentage = percentages[i];
            }

            double[] normalized = new double[span];
            for (int i = 0; i < span; i++) {
                normalized[i] = maxPercentage == 0.0 ? 0.0 : percentages[i] / maxPercentage;
            }
            out.add(new OreDistribution(e.getKey(), total, minY, maxY, counts, percentages, normalized));
        }
        return out;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public record OreDistribution(BlockId id, long totalCount, int minY, int maxY,
                                  long[] counts, double[] percentages, double[] normalized) {}
}
