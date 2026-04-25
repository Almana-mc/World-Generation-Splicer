package me.almana.wgsplicer.persistence;

import me.almana.wgsplicer.core.api.StatsRepository;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.stats.DimensionStats;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WgSplicerSavedData extends SavedData implements StatsRepository {

    public static final String DATA_ID = "oresource_stats"; // legacy id, keep stable

    private final Map<DimensionId, DimensionStats> dimensions = new LinkedHashMap<>();

    @Override
    public Map<DimensionId, DimensionStats> all() {
        return dimensions;
    }

    @Override
    public DimensionStats forDim(DimensionId key) {
        return dimensions.computeIfAbsent(key, k -> new DimensionStats());
    }

    @Override
    public void clear() {
        dimensions.clear();
        setDirty();
    }

    @Override
    public void drop(DimensionId key) {
        if (dimensions.remove(key) != null) setDirty();
    }

    @Override
    public void markDirty() {
        setDirty();
    }

    @Override
    public boolean ready() {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag out = NbtDimensionStatsCodec.writeAll(dimensions);
        tag.put("dims", out.get("dims"));
        return tag;
    }

    public static WgSplicerSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        WgSplicerSavedData data = new WgSplicerSavedData();
        Map<DimensionId, DimensionStats> loaded = NbtDimensionStatsCodec.readAll(tag);
        data.dimensions.putAll(loaded);
        return data;
    }

    public static SavedData.Factory<WgSplicerSavedData> factory() {
        return new SavedData.Factory<>(WgSplicerSavedData::new, WgSplicerSavedData::load, null);
    }
}
