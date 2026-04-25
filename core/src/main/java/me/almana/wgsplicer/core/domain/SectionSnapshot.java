package me.almana.wgsplicer.core.domain;

public final class SectionSnapshot {

    public final int baseY;
    public final BlockPalette palette;
    public final BiomeId[] biomes;

    public SectionSnapshot(int baseY, BlockPalette palette, BiomeId[] biomes) {
        this.baseY = baseY;
        this.palette = palette;
        this.biomes = biomes;
    }
}
