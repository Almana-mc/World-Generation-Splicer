package me.almana.wgsplicer.persistence;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import me.almana.wgsplicer.core.domain.BiomeId;
import me.almana.wgsplicer.core.domain.BlockId;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.stats.BlockStats;
import me.almana.wgsplicer.core.stats.DimensionStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NbtDimensionStatsCodec {

    private static final Logger LOGGER = LogUtils.getLogger();

    private NbtDimensionStatsCodec() {}

    public static CompoundTag writeDim(DimensionStats ds) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("chunksScanned", ds.chunksScanned);
        tag.putLong("blocksScanned", ds.blocksScanned);
        tag.put("scanned", new LongArrayTag(ds.scannedChunks.toLongArray()));
        ListTag list = new ListTag();
        for (Map.Entry<BlockId, BlockStats> e : ds.blocks.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", e.getKey().toString());
            BlockStats bs = e.getValue();
            entry.putLong("count", bs.totalCount);
            entry.putInt("minY", bs.minY);
            entry.putInt("maxY", bs.maxY);
            entry.putLong("firstSeen", bs.firstSeenChunk);
            entry.putLong("lastSeen", bs.lastSeenChunk);
            if (!bs.biomeCounts.isEmpty()) {
                ListTag biomeList = new ListTag();
                for (Object2LongMap.Entry<BiomeId> be : bs.biomeCounts.object2LongEntrySet()) {
                    CompoundTag bt = new CompoundTag();
                    bt.putString("biome", be.getKey().toString());
                    bt.putLong("count", be.getLongValue());
                    biomeList.add(bt);
                }
                entry.put("biomes", biomeList);
            }
            if (!bs.yBuckets.isEmpty()) {
                ListTag bucketList = new ListTag();
                for (Int2LongMap.Entry ye : bs.yBuckets.int2LongEntrySet()) {
                    CompoundTag bt = new CompoundTag();
                    bt.putInt("b", ye.getIntKey());
                    bt.putLong("count", ye.getLongValue());
                    bucketList.add(bt);
                }
                entry.put("yBuckets", bucketList);
            }
            list.add(entry);
        }
        tag.put("blocks", list);
        return tag;
    }

    public static DimensionStats readDim(CompoundTag tag) {
        DimensionStats ds = new DimensionStats();
        ds.chunksScanned = tag.getLong("chunksScanned");
        ds.blocksScanned = tag.getLong("blocksScanned");
        if (tag.contains("scanned", Tag.TAG_LONG_ARRAY)) {
            for (long l : tag.getLongArray("scanned")) ds.scannedChunks.add(l);
        }
        ListTag list = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String idStr = entry.getString("id");
            BlockId id = tryParseBlock(idStr);
            if (id == null) {
                LOGGER.warn("Skipping invalid block id in saved stats: {}", idStr);
                continue;
            }
            BlockStats bs = new BlockStats();
            bs.totalCount = entry.getLong("count");
            bs.minY = entry.contains("minY", Tag.TAG_INT) ? entry.getInt("minY") : Integer.MAX_VALUE;
            bs.maxY = entry.contains("maxY", Tag.TAG_INT) ? entry.getInt("maxY") : Integer.MIN_VALUE;
            bs.firstSeenChunk = entry.contains("firstSeen", Tag.TAG_LONG) ? entry.getLong("firstSeen") : Long.MIN_VALUE;
            bs.lastSeenChunk = entry.contains("lastSeen", Tag.TAG_LONG) ? entry.getLong("lastSeen") : Long.MIN_VALUE;
            if (entry.contains("biomes", Tag.TAG_LIST)) {
                ListTag bl = entry.getList("biomes", Tag.TAG_COMPOUND);
                for (int j = 0; j < bl.size(); j++) {
                    CompoundTag bt = bl.getCompound(j);
                    BiomeId biomeId = tryParseBiome(bt.getString("biome"));
                    if (biomeId != null) bs.biomeCounts.addTo(biomeId, bt.getLong("count"));
                }
            }
            if (entry.contains("yBuckets", Tag.TAG_LIST)) {
                ListTag yl = entry.getList("yBuckets", Tag.TAG_COMPOUND);
                for (int j = 0; j < yl.size(); j++) {
                    CompoundTag bt = yl.getCompound(j);
                    bs.yBuckets.addTo(bt.getInt("b"), bt.getLong("count"));
                }
            }
            ds.blocks.put(id, bs);
        }
        return ds;
    }

    public static CompoundTag writeAll(Map<DimensionId, DimensionStats> dims) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<DimensionId, DimensionStats> e : dims.entrySet()) {
            CompoundTag entry = writeDim(e.getValue());
            entry.putString("dim", e.getKey().toString());
            list.add(entry);
        }
        tag.put("dims", list);
        return tag;
    }

    public static Map<DimensionId, DimensionStats> readAll(CompoundTag tag) {
        Map<DimensionId, DimensionStats> dims = new LinkedHashMap<>();
        if (!tag.contains("dims", Tag.TAG_LIST)) return dims;
        ListTag list = tag.getList("dims", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            DimensionId dimId = tryParseDim(entry.getString("dim"));
            if (dimId == null) continue;
            dims.put(dimId, readDim(entry));
        }
        return dims;
    }

    private static BlockId tryParseBlock(String s) {
        if (s == null) return null;
        int colon = s.indexOf(':');
        if (colon <= 0 || colon == s.length() - 1) return null;
        return new BlockId(s.substring(0, colon), s.substring(colon + 1));
    }

    private static BiomeId tryParseBiome(String s) {
        if (s == null) return null;
        int colon = s.indexOf(':');
        if (colon <= 0 || colon == s.length() - 1) return null;
        return new BiomeId(s.substring(0, colon), s.substring(colon + 1));
    }

    private static DimensionId tryParseDim(String s) {
        if (s == null) return null;
        int colon = s.indexOf(':');
        if (colon <= 0 || colon == s.length() - 1) return null;
        return new DimensionId(s.substring(0, colon), s.substring(colon + 1));
    }
}
