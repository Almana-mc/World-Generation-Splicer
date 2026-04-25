package me.almana.wgsplicer.core.service;

import me.almana.wgsplicer.core.api.BlockRegistryView;
import me.almana.wgsplicer.core.api.ConfigSource;
import me.almana.wgsplicer.core.api.MainThreadExecutor;
import me.almana.wgsplicer.core.api.ServerPaths;
import me.almana.wgsplicer.core.api.StatsRepository;
import me.almana.wgsplicer.core.api.TextSink;
import me.almana.wgsplicer.core.api.WorldReader;
import me.almana.wgsplicer.core.domain.BlockMatcher;
import me.almana.wgsplicer.core.domain.ChunkCoord;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.domain.ExportContext;
import me.almana.wgsplicer.core.domain.ExportResultMessage;
import me.almana.wgsplicer.core.export.IncrementalJsonWriter;
import me.almana.wgsplicer.core.export.JsonStatsExporter;
import me.almana.wgsplicer.core.export.StatsExporter;
import me.almana.wgsplicer.core.scan.BlockMatcherBuilder;
import me.almana.wgsplicer.core.scan.ScanQueue;
import me.almana.wgsplicer.core.scan.ScanSession;
import me.almana.wgsplicer.core.stats.DimensionStats;
import me.almana.wgsplicer.core.stats.StatsSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class WgSplicerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WgSplicerService.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final StatsRepository repo;
    private final ScanQueue queue;
    private final WorldReader world;
    private final ConfigSource config;
    private final ServerPaths paths;
    private final MainThreadExecutor mainThread;
    private final BlockRegistryView registry;

    public WgSplicerService(StatsRepository repo, ScanQueue queue, WorldReader world,
                           ConfigSource config, ServerPaths paths,
                           MainThreadExecutor mainThread, BlockRegistryView registry) {
        this.repo = repo;
        this.queue = queue;
        this.world = world;
        this.config = config;
        this.paths = paths;
        this.mainThread = mainThread;
        this.registry = registry;
    }

    public ScanQueue queue() { return queue; }
    public StatsRepository repo() { return repo; }
    public ConfigSource config() { return config; }

    public void rebuildMatcher() {
        java.util.Set<me.almana.wgsplicer.core.domain.BlockId> oreTag = registry.blocksWithTag("c", "ores");
        BlockMatcher matcher = BlockMatcherBuilder.build(
                config.scanMode(),
                oreTag,
                config.extraBlockIds(),
                config.extraNamespaces());
        queue.setMatcher(matcher);
    }

    public void tick() {
        queue.tick();
    }

    public void status(TextSink sink) {
        if (!repo.ready()) {
            sink.failure("World Generation Splicer stats not initialized");
            return;
        }
        Map<DimensionId, DimensionStats> dims = repo.all();
        StringBuilder sb = new StringBuilder();
        sb.append("Scanning: ").append(config.scanEnabled() ? "on" : "off")
                .append(" | Mode: ").append(config.scanMode())
                .append(" | Trigger: ").append(config.scanTrigger())
                .append(" | Queued: ").append(queue.totalQueued())
                .append(" | In-flight: ").append(queue.inFlight())
                .append(" | Export dir: ").append(config.exportFolder())
                .append('\n');
        if (dims.isEmpty()) {
            sb.append("No data collected yet.");
        } else {
            for (Map.Entry<DimensionId, DimensionStats> e : dims.entrySet()) {
                DimensionStats ds = e.getValue();
                sb.append(e.getKey()).append(": ")
                        .append(ds.chunksScanned).append(" chunks, ")
                        .append(ds.blocks.size()).append(" block types, ")
                        .append(ds.blocksScanned).append(" blocks scanned\n");
            }
        }
        sink.info(sb.toString());
    }

    public void reset(TextSink sink) {
        if (!repo.ready()) {
            sink.failure("World Generation Splicer stats not initialized");
            return;
        }
        repo.clear();
        sink.info("World Generation Splicer stats cleared");
    }

    public void skipCurrent(TextSink sink) {
        DimensionId dim = queue.activeDim();
        if (dim == null) {
            sink.failure("No active dimension scan");
            return;
        }
        int dropped = queue.skipCurrent();
        sink.info("Skipped " + dropped + " queued chunks in " + dim + "; discarded collected data");
    }

    public void scanChunk(DimensionId dim, ChunkCoord pos, TextSink sink) {
        queue.enqueue(dim, pos);
        sink.info("Queued chunk " + pos.x() + "," + pos.z() + " in " + dim);
    }

    public void scanRadius(DimensionId dim, ChunkCoord center, int radius, TextSink sink) {
        int cap = config.maxManualRadius();
        int r = Math.min(radius, cap);
        ScanSession progress = new ScanSession(queue, sink.createProgressBar("World Generation Splicer scan"), "World Generation Splicer scan");
        progress.setActiveDim(dim, 0, 0);
        int queued = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                progress.enqueue(dim, new ChunkCoord(center.x() + dx, center.z() + dz));
                queued++;
            }
        }
        int total = queued;
        int budget = Math.max(1, config.maxScansPerTick());
        int estTicks = (total + budget - 1) / budget;
        sink.info("Queued " + total + " chunks (radius " + r + ", est " + estTicks + " ticks to drain)");
        long startNanos = System.nanoTime();
        progress.start();
        queue.onAllDrained(() -> {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            mainThread.execute(() -> {
                progress.finish();
                sink.info("Scan complete: " + total + " chunks in " + elapsedMs + " ms");
            });
        });
    }

    public void export(ExportContext ctx, TextSink sink) {
        if (!repo.ready()) {
            sink.failure("World Generation Splicer stats not initialized");
            return;
        }
        if (!config.exportEnabled()) {
            sink.failure("Export disabled in config");
            return;
        }
        StatsSnapshot snapshot = StatsSnapshot.of(repo.all(), world);
        String timestamp = LocalDateTime.now().format(TIMESTAMP);
        String jsonName = String.format(config.exportJsonFilenamePattern(), timestamp);
        Path folder = paths.serverDirectory().resolve(config.exportFolder());

        Runnable task = () -> runExport(ctx, sink, snapshot, folder, jsonName);

        if (config.exportAsync() && queue.executor() != null) {
            queue.executor().submit(task);
            sink.info("Export started: " + jsonName);
        } else {
            task.run();
        }
    }

    private void runExport(ExportContext ctx, TextSink sink, StatsSnapshot snapshot, Path folder, String jsonName) {
        try {
            StatsExporter.Result jsonResult = new JsonStatsExporter(config).export(snapshot, folder, jsonName, ctx);
            mainThread.execute(() -> sink.success(new ExportResultMessage(
                    jsonResult.rows(),
                    jsonResult.file(),
                    config.websiteUrl())));
        } catch (Throwable t) {
            LOGGER.error("World Generation Splicer export failed", t);
            mainThread.execute(() -> sink.failure("Export failed: " + t.getMessage()));
        }
    }

    public void scanAll(int radius, ExportContext ctx, TextSink sink) {
        if (!repo.ready()) {
            sink.failure("World Generation Splicer stats not initialized");
            return;
        }
        List<DimensionId> dims = world.dimensions();
        if (dims.isEmpty()) {
            sink.failure("No dimensions available (all blacklisted?)");
            return;
        }
        java.util.ArrayList<DimensionId> remaining = new java.util.ArrayList<>(dims);
        int dimCount = remaining.size();
        int r = radius;
        int perDim = (2 * r + 1) * (2 * r + 1);
        sink.info("Bulk scan starting: " + dimCount + " dimensions, radius " + r + " (~" + perDim + " chunks each)");

        queue.setBulkBudget(256);
        queue.setBulkInFlightCap(256);

        String timestamp = LocalDateTime.now().format(TIMESTAMP);
        String jsonName = String.format(config.exportJsonFilenamePattern(), timestamp);
        Path folder = paths.serverDirectory().resolve(config.exportFolder());

        IncrementalJsonWriter writer = new IncrementalJsonWriter(config);
        try {
            writer.begin(folder, jsonName, config.yLevelBucketSize(), ctx);
        } catch (Throwable t) {
            LOGGER.error("Failed to open streaming JSON export", t);
            queue.clearBulkBudget();
            queue.clearBulkInFlightCap();
            sink.failure("Failed to open JSON export: " + t.getMessage());
            return;
        }

        boolean[] skipped = {false};
        queue.setSkipListener(d -> skipped[0] = true);

        ScanSession progress = new ScanSession(queue, sink.createProgressBar("World Generation Splicer scan"), "World Generation Splicer scan");
        int[] dimIdx = {0};

        Runnable[] starter = new Runnable[1];
        long startNanos = System.nanoTime();
        starter[0] = () -> {
            DimensionId finished = queue.activeDim();
            if (finished != null) {
                if (!skipped[0]) {
                    try {
                        DimensionStats stats = repo.all().get(finished);
                        if (stats != null) {
                            StatsSnapshot dimSnap = StatsSnapshot.of(Map.of(finished, stats), world);
                            writer.appendDim(finished, dimSnap.dims.get(finished));
                        }
                    } catch (Throwable t) {
                        LOGGER.error("Per-dim JSON append failed for {}", finished, t);
                        sink.failure("JSON append failed for " + finished + ": " + t.getMessage());
                    }
                }
                repo.drop(finished);
                skipped[0] = false;
            }

            if (remaining.isEmpty()) {
                Path jsonPath = null;
                try {
                    jsonPath = writer.finish();
                } catch (Throwable t) {
                    LOGGER.error("JSON finish failed", t);
                    writer.abort();
                    sink.failure("JSON finalize failed: " + t.getMessage());
                }
                queue.clearSkipListener();
                queue.clearBulkBudget();
                queue.clearBulkInFlightCap();
                queue.clearActiveDim();
                progress.finish();
                long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
                sink.info("Bulk scan complete in " + elapsedMs + " ms");
                if (jsonPath != null) {
                    int oreRows = writer.oreRows();
                    sink.success(new ExportResultMessage(oreRows, jsonPath, config.websiteUrl()));
                }
                return;
            }
            DimensionId dimKey = remaining.remove(0);
            queue.setActiveDim(dimKey);
            dimIdx[0]++;
            progress.setActiveDim(dimKey, dimIdx[0], dimCount);
            ChunkCoord center = world.spawnChunk(dimKey);
            int queued = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    progress.enqueue(dimKey, new ChunkCoord(center.x() + dx, center.z() + dz));
                    queued++;
                }
            }
            int total = queued;
            sink.info("[" + dimKey + "] queued " + total + " chunks around " + center.x() + "," + center.z());
            queue.onAllDrained(() -> mainThread.execute(starter[0]));
        };
        progress.start();
        starter[0].run();
    }
}
