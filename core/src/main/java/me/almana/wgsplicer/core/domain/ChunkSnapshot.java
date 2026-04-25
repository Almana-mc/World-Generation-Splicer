package me.almana.wgsplicer.core.domain;

import java.util.List;

public final class ChunkSnapshot {

    public final DimensionId dim;
    public final ChunkCoord pos;
    public final List<SectionSnapshot> sections;
    public final int totalNonAirSections;

    public ChunkSnapshot(DimensionId dim, ChunkCoord pos, List<SectionSnapshot> sections, int totalNonAirSections) {
        this.dim = dim;
        this.pos = pos;
        this.sections = sections;
        this.totalNonAirSections = totalNonAirSections;
    }
}
