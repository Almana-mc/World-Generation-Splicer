package me.almana.wgsplicer.core.scan;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.almana.wgsplicer.core.api.ConfigSource;
import me.almana.wgsplicer.core.api.MainThreadExecutor;
import me.almana.wgsplicer.core.api.StatsRepository;
import me.almana.wgsplicer.core.api.WorldReader;
import me.almana.wgsplicer.core.domain.BlockMatcher;
import me.almana.wgsplicer.core.domain.ChunkCoord;
import me.almana.wgsplicer.core.domain.ChunkSnapshot;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.stats.DimensionStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class ScanQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanQueue.class);

    private final ConfigSource config;
    private final WorldReader world;
    private final MainThreadExecutor mainThread;
    private final StatsRepository repo;

    private final Map<DimensionId, ArrayDeque<ChunkCoord>> queues = new HashMap<>();
    private final Map<DimensionId, LongOpenHashSet> queuedSet = new HashMap<>();
    private final Map<DimensionId, Long2ObjectOpenHashMap<ScanSession>> chunkOwners = new HashMap<>();
    private final List<ScanSession> sessions = new ArrayList<>();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final List<Runnable> drainCallbacks = new ArrayList<>();

    private ExecutorService executor;
    private volatile DimensionId activeDim;
    private volatile int bulkBudgetOverride;
    private volatile int bulkInFlightCapOverride;
    private volatile Consumer<DimensionId> skipListener;
    private volatile BlockMatcher matcher = id -> false;

    public ScanQueue(ConfigSource config, WorldReader world, MainThreadExecutor mainThread, StatsRepository repo) {
        this.config = config;
        this.world = world;
        this.mainThread = mainThread;
        this.repo = repo;
    }

    public void start() {
        if (executor == null || executor.isShutdown()) {
            int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
            executor = Executors.newFixedThreadPool(threads, namedThreadFactory());
        }
        LOGGER.info("[WGSplicer] ScanQueue started, workers={}", executor);
    }

    public void stop() {
        queues.clear();
        queuedSet.clear();
        chunkOwners.clear();
        sessions.clear();
        inFlight.set(0);
        activeDim = null;
        bulkBudgetOverride = 0;
        bulkInFlightCapOverride = 0;
        skipListener = null;
        synchronized (drainCallbacks) { drainCallbacks.clear(); }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    public void setMatcher(BlockMatcher matcher) {
        this.matcher = matcher;
    }

    public BlockMatcher matcher() {
        return matcher;
    }

    public void registerSession(ScanSession session) {
        sessions.add(session);
    }

    public void unregisterSession(ScanSession session) {
        sessions.remove(session);
    }

    public void registerOwner(DimensionId dim, long posLong, ScanSession owner) {
        chunkOwners.computeIfAbsent(dim, k -> new Long2ObjectOpenHashMap<>()).put(posLong, owner);
    }

    public void onAllDrained(Runnable cb) {
        synchronized (drainCallbacks) { drainCallbacks.add(cb); }
    }

    public void setActiveDim(DimensionId dim) { activeDim = dim; }
    public void clearActiveDim() { activeDim = null; }
    public DimensionId activeDim() { return activeDim; }

    public int skipCurrent() {
        DimensionId dim = activeDim;
        if (dim == null) return 0;
        ArrayDeque<ChunkCoord> q = queues.get(dim);
        Long2ObjectOpenHashMap<ScanSession> owners = chunkOwners.get(dim);
        int dropped = 0;
        if (q != null) {
            for (ChunkCoord pos : q) {
                long posLong = pos.packed();
                ScanSession owner = owners == null ? null : owners.remove(posLong);
                if (owner != null) owner.onChunkCompleted();
                dropped++;
            }
            q.clear();
        }
        LongOpenHashSet set = queuedSet.get(dim);
        if (set != null) set.clear();
        repo.drop(dim);
        Consumer<DimensionId> l = skipListener;
        if (l != null) l.accept(dim);
        return dropped;
    }

    public void setSkipListener(Consumer<DimensionId> l) { skipListener = l; }
    public void clearSkipListener() { skipListener = null; }
    public void setBulkBudget(int b) { bulkBudgetOverride = Math.max(0, b); }
    public void clearBulkBudget() { bulkBudgetOverride = 0; }
    public void setBulkInFlightCap(int c) { bulkInFlightCapOverride = Math.max(0, c); }
    public void clearBulkInFlightCap() { bulkInFlightCapOverride = 0; }
    public ExecutorService executor() { return executor; }

    public int totalQueued() {
        int t = 0;
        for (ArrayDeque<ChunkCoord> q : queues.values()) t += q.size();
        return t;
    }

    public int inFlight() { return inFlight.get(); }

    public void enqueue(DimensionId dim, ChunkCoord pos) {
        LongOpenHashSet queued = queuedSet.computeIfAbsent(dim, k -> new LongOpenHashSet());
        if (!queued.add(pos.packed())) return;
        queues.computeIfAbsent(dim, k -> new ArrayDeque<>()).add(pos);
    }

    public void tick() {
        if (!config.scanEnabled() || !repo.ready()) return;
        int budget = bulkBudgetOverride > 0 ? bulkBudgetOverride : config.maxScansPerTick();
        int cap = bulkInFlightCapOverride > 0 ? bulkInFlightCapOverride : config.maxInFlightWorkers();

        for (int i = 0; i < budget; i++) {
            if (inFlight.get() >= cap) break;
            if (!drainOne()) break;
        }

        if (!sessions.isEmpty()) {
            for (int i = sessions.size() - 1; i >= 0; i--) {
                ScanSession s = sessions.get(i);
                if (s.isFinished()) sessions.remove(i);
                else s.tickUpdate();
            }
        }

        DimensionId pinned = activeDim;
        int relevantQueued;
        if (pinned != null) {
            ArrayDeque<ChunkCoord> q = queues.get(pinned);
            relevantQueued = q == null ? 0 : q.size();
        } else {
            relevantQueued = totalQueued();
        }
        if (relevantQueued == 0 && inFlight.get() == 0) {
            List<Runnable> fire;
            synchronized (drainCallbacks) {
                if (drainCallbacks.isEmpty()) return;
                fire = new ArrayList<>(drainCallbacks);
                drainCallbacks.clear();
            }
            for (Runnable r : fire) {
                try { r.run(); } catch (Throwable t) { LOGGER.error("Drain callback failed", t); }
            }
        }
    }

    private boolean drainOne() {
        DimensionId pinned = activeDim;
        if (pinned != null) {
            ArrayDeque<ChunkCoord> queue = queues.get(pinned);
            if (queue == null || queue.isEmpty()) return false;
            ChunkCoord pos = queue.pollFirst();
            queuedSet.get(pinned).remove(pos.packed());
            processOne(pinned, pos);
            return true;
        }
        for (Map.Entry<DimensionId, ArrayDeque<ChunkCoord>> entry : queues.entrySet()) {
            ArrayDeque<ChunkCoord> queue = entry.getValue();
            if (queue.isEmpty()) continue;
            ChunkCoord pos = queue.pollFirst();
            queuedSet.get(entry.getKey()).remove(pos.packed());
            processOne(entry.getKey(), pos);
            return true;
        }
        return false;
    }

    private void processOne(DimensionId dim, ChunkCoord pos) {
        long posLong = pos.packed();
        Long2ObjectOpenHashMap<ScanSession> dimOwners = chunkOwners.get(dim);
        ScanSession owner = dimOwners == null ? null : dimOwners.remove(posLong);

        DimensionStats ds = repo.forDim(dim);
        if (!config.rescanAlreadyScanned() && ds.isScanned(posLong)) {
            if (owner != null) owner.onChunkCompleted();
            return;
        }

        boolean forceLoad = bulkBudgetOverride > 0;
        ChunkSnapshot snap = world.snapshotChunk(dim, pos.x(), pos.z(), forceLoad, matcher);
        if (snap == null) {
            if (owner != null) owner.onChunkCompleted();
            return;
        }

        if (snap.sections.isEmpty()) {
            ds.markChunkScanned(posLong);
            ds.chunksScanned += 1;
            repo.markDirty();
            if (owner != null) owner.onChunkCompleted();
            return;
        }

        int yBucketSize = config.includeYLevelStats() ? config.yLevelBucketSize() : 0;
        boolean biomeEnabled = config.includeBiomeStats();
        BlockMatcher m = matcher;

        inFlight.incrementAndGet();
        try {
            executor.submit(() -> {
                try {
                    ChunkScanWorker.scan(snap, m, yBucketSize, biomeEnabled, mainThread, repo);
                } catch (Throwable t) {
                    LOGGER.error("Chunk scan worker failed for {} @ {}", dim, pos, t);
                } finally {
                    inFlight.decrementAndGet();
                    if (owner != null) owner.onChunkCompleted();
                }
            });
        } catch (Throwable t) {
            inFlight.decrementAndGet();
            if (owner != null) owner.onChunkCompleted();
            LOGGER.error("Failed to submit chunk scan worker", t);
        }
    }

    private static ThreadFactory namedThreadFactory() {
        final AtomicInteger idx = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, "WGSplicer-Worker-" + idx.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }
}
