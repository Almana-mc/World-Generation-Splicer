package me.almana.wgsplicer.adapter;

import me.almana.wgsplicer.core.api.BlockRegistryView;
import me.almana.wgsplicer.core.domain.BlockId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class NeoForgeBlockRegistryView implements BlockRegistryView {

    @Override
    public boolean exists(BlockId id) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path());
        return BuiltInRegistries.BLOCK.containsKey(rl);
    }

    @Override
    public Set<BlockId> blocksWithTag(String tagNamespace, String tagPath) {
        TagKey<Block> tag = TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(tagNamespace, tagPath));
        Set<BlockId> out = new HashSet<>();
        Optional<? extends Iterable<net.minecraft.core.Holder<Block>>> opt = BuiltInRegistries.BLOCK.getTag(tag);
        if (opt.isEmpty()) return out;
        opt.get().forEach(holder -> {
            ResourceLocation rl = holder.unwrapKey().map(k -> k.location()).orElse(null);
            if (rl != null) out.add(new BlockId(rl.getNamespace(), rl.getPath()));
        });
        return out;
    }
}
