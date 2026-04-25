package me.almana.wgsplicer.core.domain;

@FunctionalInterface
public interface BlockMatcher {
    boolean matches(BlockId id);
}
