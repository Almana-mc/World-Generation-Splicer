package me.almana.wgsplicer.core.domain;

@FunctionalInterface
public interface CellVisitor {
    void visit(int lx, int ly, int lz, BlockId id);
}
