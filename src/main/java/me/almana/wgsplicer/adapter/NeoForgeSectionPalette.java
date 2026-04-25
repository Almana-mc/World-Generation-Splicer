package me.almana.wgsplicer.adapter;

import me.almana.wgsplicer.core.domain.BlockId;
import me.almana.wgsplicer.core.domain.BlockMatcher;
import me.almana.wgsplicer.core.domain.BlockPalette;
import me.almana.wgsplicer.core.domain.CellVisitor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

public final class NeoForgeSectionPalette implements BlockPalette {

    private final PalettedContainer<BlockState> states;

    public NeoForgeSectionPalette(PalettedContainer<BlockState> states) {
        this.states = states;
    }

    @Override
    public void forEachMatching(BlockMatcher matcher, CellVisitor visitor) {
        for (int ly = 0; ly < 16; ly++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    BlockState st = states.get(lx, ly, lz);
                    if (st.isAir()) continue;
                    BlockId id = blockIdOf(st);
                    if (id == null) continue;
                    if (!matcher.matches(id)) continue;
                    visitor.visit(lx, ly, lz, id);
                }
            }
        }
    }

    private static BlockId blockIdOf(BlockState state) {
        ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (rl == null) return null;
        return new BlockId(rl.getNamespace(), rl.getPath());
    }
}
