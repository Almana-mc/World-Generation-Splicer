package me.almana.wgsplicer.core.scan;

import me.almana.wgsplicer.core.api.ScanMode;
import me.almana.wgsplicer.core.domain.BlockId;
import me.almana.wgsplicer.core.domain.BlockMatcher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockMatcherBuilderTest {

    private static final BlockId DIAMOND = new BlockId("minecraft", "diamond_ore");
    private static final BlockId IRON = new BlockId("minecraft", "iron_ore");
    private static final BlockId STONE = new BlockId("minecraft", "stone");
    private static final BlockId CREATE_BRASS = new BlockId("create", "brass_ingot");

    @Test
    void oresOnlyMatchesTagSet() {
        BlockMatcher m = BlockMatcherBuilder.build(ScanMode.ORES_ONLY,
                Set.of(DIAMOND, IRON), List.of(), List.of());
        assertTrue(m.matches(DIAMOND));
        assertTrue(m.matches(IRON));
        assertFalse(m.matches(STONE));
        assertFalse(m.matches(CREATE_BRASS));
    }

    @Test
    void configuredOnlyMatchesExtraIdsAndNamespaces() {
        BlockMatcher m = BlockMatcherBuilder.build(ScanMode.CONFIGURED_ONLY,
                Set.of(DIAMOND), List.of("minecraft:stone"), List.of("create"));
        assertTrue(m.matches(STONE));
        assertTrue(m.matches(CREATE_BRASS));
        assertFalse(m.matches(DIAMOND));
        assertFalse(m.matches(IRON));
    }

    @Test
    void oresAndConfiguredUnion() {
        BlockMatcher m = BlockMatcherBuilder.build(ScanMode.ORES_AND_CONFIGURED,
                Set.of(DIAMOND), List.of("minecraft:stone"), List.of());
        assertTrue(m.matches(DIAMOND));
        assertTrue(m.matches(STONE));
        assertFalse(m.matches(IRON));
    }

    @Test
    void allBlocksDebugMatchesEverything() {
        BlockMatcher m = BlockMatcherBuilder.build(ScanMode.ALL_BLOCKS_DEBUG,
                Set.of(), List.of(), List.of());
        assertTrue(m.matches(DIAMOND));
        assertTrue(m.matches(STONE));
        assertTrue(m.matches(CREATE_BRASS));
    }

    @Test
    void invalidBlockIdsIgnored() {
        BlockMatcher m = BlockMatcherBuilder.build(ScanMode.CONFIGURED_ONLY,
                Set.of(), List.of("nope", ":bad", "good:bad:extra"), List.of());
        assertFalse(m.matches(STONE));
    }
}
