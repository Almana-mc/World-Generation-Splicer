package me.almana.wgsplicer.core.scan;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.almana.wgsplicer.core.api.ProgressBar;
import me.almana.wgsplicer.core.domain.ChunkCoord;
import me.almana.wgsplicer.core.domain.DimensionId;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class ScanSession {

    private final ScanQueue queue;
    private final ProgressBar bar;
    private final String label;
    private final Map<DimensionId, LongOpenHashSet> ownedChunks = new HashMap<>();
    private final AtomicInteger processed = new AtomicInteger();
    private int totalEnqueued;
    private DimensionId activeDim;
    private int dimIndex;
    private int dimTotal;
    private int lastSentDone = -1;
    private boolean started;
    private boolean finished;

    public ScanSession(ScanQueue queue, ProgressBar bar, String label) {
        this.queue = queue;
        this.bar = bar;
        this.label = label;
    }

    public void start() {
        if (started) return;
        started = true;
        if (bar != null) bar.setProgress(0f, buildTitle(0, totalEnqueued));
        queue.registerSession(this);
    }

    public void enqueue(DimensionId dim, ChunkCoord pos) {
        long key = pos.packed();
        LongOpenHashSet set = ownedChunks.computeIfAbsent(dim, k -> new LongOpenHashSet());
        if (!set.add(key)) return;
        totalEnqueued++;
        queue.registerOwner(dim, key, this);
        queue.enqueue(dim, pos);
    }

    public void setActiveDim(DimensionId dim, int index, int total) {
        this.activeDim = dim;
        this.dimIndex = index;
        this.dimTotal = total;
    }

    public void onChunkCompleted() {
        processed.incrementAndGet();
    }

    public void tickUpdate() {
        if (finished || !started || bar == null) return;
        int done = processed.get();
        if (done == lastSentDone) return;
        lastSentDone = done;
        int total = totalEnqueued;
        float frac = total == 0 ? 0f : Math.min(1f, done / (float) total);
        bar.setProgress(frac, buildTitle(done, total));
    }

    public void finish() {
        if (finished) return;
        finished = true;
        if (bar != null) bar.close();
        ownedChunks.clear();
        queue.unregisterSession(this);
    }

    public boolean isFinished() {
        return finished;
    }

    private String buildTitle(int done, int total) {
        StringBuilder sb = new StringBuilder(label);
        if (activeDim != null) {
            sb.append(" [").append(activeDim);
            if (dimTotal > 0) sb.append(' ').append(dimIndex).append('/').append(dimTotal);
            sb.append(']');
        }
        sb.append(' ').append(done).append(" / ").append(total);
        return sb.toString();
    }
}
