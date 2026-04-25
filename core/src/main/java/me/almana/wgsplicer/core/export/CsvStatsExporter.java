package me.almana.wgsplicer.core.export;

import me.almana.wgsplicer.core.domain.BlockId;
import me.almana.wgsplicer.core.domain.ChunkCoord;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.domain.ExportContext;
import me.almana.wgsplicer.core.stats.StatsSnapshot;
import me.almana.wgsplicer.core.stats.StatsSnapshot.BlockSnapshot;
import me.almana.wgsplicer.core.stats.StatsSnapshot.DimSnapshot;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class CsvStatsExporter implements StatsExporter {

    @Override
    public Result export(StatsSnapshot snapshot, Path folder, String filename, ExportContext ctx) throws Exception {
        Files.createDirectories(folder);
        String csvName = filename.endsWith(".xlsx") ? filename.replace(".xlsx", ".csv") : filename + ".csv";
        Path target = folder.resolve(csvName);

        int rows = 0;
        try (BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            w.write("Dimension,BlockId,TotalCount,ChunksScanned,AveragePerChunk,MinY,MaxY,FirstSeenChunk,LastSeenChunk");
            w.newLine();
            for (Map.Entry<DimensionId, DimSnapshot> de : snapshot.dims.entrySet()) {
                String dim = de.getKey().toString();
                DimSnapshot ds = de.getValue();
                for (Map.Entry<BlockId, BlockSnapshot> be : ds.blocks.entrySet()) {
                    BlockSnapshot b = be.getValue();
                    double avg = ds.chunksScanned == 0 ? 0.0 : (double) b.totalCount / ds.chunksScanned;
                    w.write(dim + "," + be.getKey() + "," + b.totalCount + "," + ds.chunksScanned
                            + "," + avg
                            + "," + (b.minY == Integer.MAX_VALUE ? "" : b.minY)
                            + "," + (b.maxY == Integer.MIN_VALUE ? "" : b.maxY)
                            + "," + formatChunk(b.firstSeenChunk)
                            + "," + formatChunk(b.lastSeenChunk));
                    w.newLine();
                    rows++;
                }
            }
        }
        return new Result(target, rows, true, "Wrote " + rows + " rows");
    }

    private static String formatChunk(long chunkPos) {
        if (chunkPos == Long.MIN_VALUE) return "";
        ChunkCoord cp = ChunkCoord.fromPacked(chunkPos);
        return cp.x() + ":" + cp.z();
    }
}
