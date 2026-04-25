package me.almana.wgsplicer.core.api;

import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.stats.DimensionStats;

import java.util.Map;

public interface StatsRepository {

    DimensionStats forDim(DimensionId dim);

    Map<DimensionId, DimensionStats> all();

    void drop(DimensionId dim);

    void clear();

    void markDirty();

    boolean ready();
}
