package me.almana.wgsplicer.core.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.almana.wgsplicer.core.api.ConfigSource;
import me.almana.wgsplicer.core.domain.BlockId;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.domain.ExportContext;
import me.almana.wgsplicer.core.export.OreDistributionCalculator.OreDistribution;
import me.almana.wgsplicer.core.stats.StatsSnapshot;
import me.almana.wgsplicer.core.stats.StatsSnapshot.BlockSnapshot;
import me.almana.wgsplicer.core.stats.StatsSnapshot.DimSnapshot;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonStatsExporter implements StatsExporter {

    static final int MIN_STRUCTURE_NOISE_TOTAL = 8;
    static final double MIN_STRUCTURE_NOISE_PER_CHUNK = 0.01;

    private final ConfigSource config;

    public JsonStatsExporter(ConfigSource config) {
        this.config = config;
    }

    @Override
    public Result export(StatsSnapshot snapshot, Path folder, String filename, ExportContext ctx) throws Exception {
        Files.createDirectories(folder);
        Path target = dedupe(folder.resolve(filename));

        int bucketSize = Math.max(1, config.yLevelBucketSize());
        int oreRows = 0;

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("meta", buildMeta(bucketSize, ctx));

        Map<String, Object> dims = new LinkedHashMap<>();
        for (Map.Entry<DimensionId, DimSnapshot> de : snapshot.dims.entrySet()) {
            DimensionId dimId = de.getKey();
            Map<String, Object> dim = buildDimEntry(dimId, de.getValue(), bucketSize);
            if (dim == null) continue;
            dims.put(dimKey(dimId), dim);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ores = (List<Map<String, Object>>) dim.get("ores");
            oreRows += ores.size();
        }
        root.put("dimensions", dims);

        Gson gson = config.exportJsonPretty()
                ? new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                : new GsonBuilder().disableHtmlEscaping().create();

        try (BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            gson.toJson(root, w);
        }
        return new Result(target, oreRows, true, "Wrote " + oreRows + " ore entries");
    }

    public static Map<String, Object> buildDimEntry(DimensionId dimId, DimSnapshot ds, int bucketSize) {
        if (!ds.hasYBounds) return null;

        int dimMinY = ds.minY;
        int dimMaxY = ds.maxY - 1;

        List<Map.Entry<BlockId, BlockSnapshot>> blockEntries = new ArrayList<>(ds.blocks.entrySet());
        blockEntries.removeIf(e -> shouldSkipOverworldOre(dimId, e.getKey(), e.getValue(), ds.chunksScanned));
        blockEntries.sort(Comparator.<Map.Entry<BlockId, BlockSnapshot>>comparingLong(e -> -e.getValue().totalCount)
                .thenComparing(e -> e.getKey().toString()));

        List<OreDistribution> dists = OreDistributionCalculator.calculate(blockEntries, bucketSize, dimMinY, dimMaxY);

        List<Map<String, Object>> ores = new ArrayList<>();
        for (OreDistribution d : dists) {
            BlockSnapshot bs = ds.blocks.get(d.id());
            if (bs == null) continue;
            Map<String, Object> ore = buildOre(d, bs, dimMinY, dimMaxY, ds.chunksScanned);
            if (ore == null) continue;
            ores.add(ore);
        }

        Map<String, Object> dim = new LinkedHashMap<>();
        dim.put("id", dimId.toString());
        dim.put("label", dimLabel(dimId));
        dim.put("minY", dimMinY);
        dim.put("maxY", dimMaxY);
        dim.put("chunksScanned", ds.chunksScanned);
        dim.put("blocksScanned", ds.blocksScanned);
        dim.put("ores", ores);
        return dim;
    }

    public static Map<String, Object> buildMeta(int bucketSize, ExportContext ctx) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("modVersion", ctx.modVersion());
        meta.put("mcVersion", ctx.mcVersion());
        meta.put("loader", ctx.loader());
        meta.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        meta.put("bucketSize", bucketSize);
        return meta;
    }

    static Map<String, Object> buildOre(OreDistribution d, BlockSnapshot bs, int dimMinY, int dimMaxY, long chunksScanned) {
        long[] fullCounts = d.counts();
        double[] fullPct = d.percentages();
        double[] fullNorm = d.normalized();

        int oreMinY = bs.minY != Integer.MAX_VALUE ? Math.max(bs.minY, dimMinY) : dimMinY;
        int oreMaxY = bs.maxY != Integer.MIN_VALUE ? Math.min(bs.maxY, dimMaxY) : dimMaxY;
        if (oreMaxY < oreMinY) return null;

        int sliceLen = oreMaxY - oreMinY + 1;
        long[] counts = new long[sliceLen];
        double[] percentages = new double[sliceLen];
        double[] normalized = new double[sliceLen];
        for (int i = 0; i < sliceLen; i++) {
            int fullIdx = (oreMinY + i) - dimMinY;
            if (fullIdx >= 0 && fullIdx < fullCounts.length) {
                counts[i] = fullCounts[fullIdx];
                percentages[i] = fullPct[fullIdx];
                normalized[i] = fullNorm[fullIdx];
            }
        }

        List<Map<String, Object>> ranges = detectRanges(counts, oreMinY);

        double perChunk = chunksScanned == 0 ? 0.0 : (double) d.totalCount() / chunksScanned;

        Map<String, Object> ore = new LinkedHashMap<>();
        ore.put("id", d.id().toString());
        ore.put("label", OreGraphPalette.nameLabel(d.id()));
        ore.put("color", OreGraphPalette.hexColor(d.id()));
        ore.put("minY", oreMinY);
        ore.put("maxY", oreMaxY);
        ore.put("counts", counts);
        ore.put("percentages", percentages);
        ore.put("normalized", normalized);
        ore.put("ranges", ranges);
        ore.put("perChunk", roundTo(perChunk, 4));
        ore.put("totalCount", d.totalCount());
        return ore;
    }

    static boolean shouldSkipOverworldOre(DimensionId dimId, BlockId blockId, BlockSnapshot block, long chunksScanned) {
        if (!"minecraft:overworld".equals(dimId.toString())) return false;
        return isNetherOre(blockId) || isSparseOre(block, chunksScanned);
    }

    static boolean isNetherOre(BlockId blockId) {
        String id = blockId.toString();
        return id.contains("nether") && id.contains("ore");
    }

    static boolean isSparseOre(BlockSnapshot block, long chunksScanned) {
        if (chunksScanned <= 0L) return false;
        double limit = Math.max(MIN_STRUCTURE_NOISE_TOTAL, chunksScanned * MIN_STRUCTURE_NOISE_PER_CHUNK);
        return block.totalCount < limit;
    }

    static List<Map<String, Object>> detectRanges(long[] counts, int baseY) {
        List<Map<String, Object>> out = new ArrayList<>();
        boolean inside = false;
        int start = 0;
        for (int i = 0; i < counts.length; i++) {
            boolean hasCount = counts[i] > 0L;
            if (hasCount && !inside) {
                start = baseY + i;
                inside = true;
            } else if (!hasCount && inside) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("minY", start);
                r.put("maxY", baseY + i - 1);
                out.add(r);
                inside = false;
            }
        }
        if (inside) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("minY", start);
            r.put("maxY", baseY + counts.length - 1);
            out.add(r);
        }
        if (out.isEmpty()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("minY", baseY);
            r.put("maxY", baseY + counts.length - 1);
            out.add(r);
        }
        return out;
    }

    public static String dimKey(DimensionId id) {
        return id.path();
    }

    static String dimLabel(DimensionId id) {
        String path = id.path();
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) sb.append(parts[i].substring(1));
        }
        return sb.length() == 0 ? path : sb.toString();
    }

    static double roundTo(double v, int places) {
        double p = Math.pow(10, places);
        return Math.round(v * p) / p;
    }

    public static Path dedupe(Path target) {
        if (!Files.exists(target)) return target;
        String name = target.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        String ext = dot >= 0 ? name.substring(dot) : "";
        for (int i = 1; i < 1000; i++) {
            Path alt = target.resolveSibling(base + "-" + i + ext);
            if (!Files.exists(alt)) return alt;
        }
        return target;
    }
}
