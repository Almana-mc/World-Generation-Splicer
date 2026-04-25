package me.almana.wgsplicer.core.domain;

public interface BlockPalette {
    void forEachMatching(BlockMatcher matcher, CellVisitor visitor);
}
