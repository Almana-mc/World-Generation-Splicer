package me.almana.wgsplicer.core.domain;

public record ChunkCoord(int x, int z) {

    public long packed() {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    public static ChunkCoord fromPacked(long packed) {
        return new ChunkCoord((int) (packed & 0xFFFFFFFFL), (int) ((packed >>> 32) & 0xFFFFFFFFL));
    }
}
