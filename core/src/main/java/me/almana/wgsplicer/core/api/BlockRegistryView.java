package me.almana.wgsplicer.core.api;

import me.almana.wgsplicer.core.domain.BlockId;

import java.util.Set;

public interface BlockRegistryView {

    boolean exists(BlockId id);

    Set<BlockId> blocksWithTag(String tagNamespace, String tagPath);
}
