package me.almana.wgsplicer.core.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import me.almana.wgsplicer.core.api.ConfigSource;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.domain.ExportContext;
import me.almana.wgsplicer.core.stats.StatsSnapshot.DimSnapshot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class IncrementalJsonWriter {

    private final ConfigSource config;

    private Path target;
    private BufferedWriter buffered;
    private JsonWriter writer;
    private Gson gson;
    private int bucketSize;
    private int oreRows;
    private boolean inDimensions;

    public IncrementalJsonWriter(ConfigSource config) {
        this.config = config;
    }

    public Path begin(Path folder, String filename, int bucketSize, ExportContext ctx) throws IOException {
        Files.createDirectories(folder);
        this.target = JsonStatsExporter.dedupe(folder.resolve(filename));
        this.bucketSize = Math.max(1, bucketSize);
        this.oreRows = 0;

        boolean pretty = config.exportJsonPretty();
        GsonBuilder gb = new GsonBuilder().disableHtmlEscaping();
        if (pretty) gb.setPrettyPrinting();
        this.gson = gb.create();

        this.buffered = Files.newBufferedWriter(target, StandardCharsets.UTF_8);
        this.writer = new JsonWriter(buffered);
        if (pretty) writer.setIndent("  ");

        writer.beginObject();
        writer.name("meta");
        gson.toJson(JsonStatsExporter.buildMeta(this.bucketSize, ctx), Map.class, writer);
        writer.name("dimensions");
        writer.beginObject();
        inDimensions = true;
        return target;
    }

    public void appendDim(DimensionId dimId, DimSnapshot ds) throws IOException {
        if (!inDimensions) throw new IllegalStateException("appendDim called before begin or after finish");
        Map<String, Object> entry = JsonStatsExporter.buildDimEntry(dimId, ds, bucketSize);
        if (entry == null) return;
        writer.name(JsonStatsExporter.dimKey(dimId));
        gson.toJson(entry, Map.class, writer);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ores = (List<Map<String, Object>>) entry.get("ores");
        oreRows += ores.size();
    }

    public Path finish() throws IOException {
        if (!inDimensions) throw new IllegalStateException("finish called twice or before begin");
        writer.endObject();
        writer.endObject();
        writer.flush();
        writer.close();
        inDimensions = false;
        return target;
    }

    public int oreRows() {
        return oreRows;
    }

    public Path target() {
        return target;
    }

    public void abort() {
        try {
            if (writer != null) writer.close();
        } catch (IOException ignored) {
        }
        try {
            if (target != null) Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
        inDimensions = false;
    }
}
