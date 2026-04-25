package me.almana.wgsplicer.core.api;

import me.almana.wgsplicer.core.domain.BlockMatcher;
import me.almana.wgsplicer.core.domain.ChunkCoord;
import me.almana.wgsplicer.core.domain.ChunkSnapshot;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.domain.YBounds;

import java.util.List;

public interface WorldReader {

    List<DimensionId> dimensions();

    YBounds yBounds(DimensionId dim);

    ChunkCoord spawnChunk(DimensionId dim);

    ChunkSnapshot snapshotChunk(DimensionId dim, int chunkX, int chunkZ, boolean forceLoad, BlockMatcher matcher);
}
