package me.almana.wgsplicer.adapter;

import me.almana.wgsplicer.core.api.ConfigSource;
import me.almana.wgsplicer.core.api.WorldReader;
import me.almana.wgsplicer.core.domain.BiomeId;
import me.almana.wgsplicer.core.domain.BlockId;
import me.almana.wgsplicer.core.domain.BlockMatcher;
import me.almana.wgsplicer.core.domain.ChunkCoord;
import me.almana.wgsplicer.core.domain.ChunkSnapshot;
import me.almana.wgsplicer.core.domain.DimensionId;
import me.almana.wgsplicer.core.domain.SectionSnapshot;
import me.almana.wgsplicer.core.domain.YBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.List;

public final class NeoForgeWorldReader implements WorldReader {

    private final MinecraftServer server;
    private final ConfigSource config;

    public NeoForgeWorldReader(MinecraftServer server, ConfigSource config) {
        this.server = server;
        this.config = config;
    }

    @Override
    public List<DimensionId> dimensions() {
        List<String> blacklist = config.dimensionBlacklist();
        List<DimensionId> out = new ArrayList<>();
        server.getAllLevels().forEach(level -> {
            ResourceLocation rl = level.dimension().location();
            String full = rl.getNamespace() + ":" + rl.getPath();
            if (blacklist.contains(full)) return;
            out.add(new DimensionId(rl.getNamespace(), rl.getPath()));
        });
        return out;
    }

    @Override
    public YBounds yBounds(DimensionId dim) {
        ServerLevel level = level(dim);
        if (level == null) return null;
        return new YBounds(level.getMinBuildHeight(), level.getMaxBuildHeight());
    }

    @Override
    public ChunkCoord spawnChunk(DimensionId dim) {
        ServerLevel level = level(dim);
        if (level == null) return new ChunkCoord(0, 0);
        BlockPos sp = level.getSharedSpawnPos();
        if (sp == null) sp = BlockPos.ZERO;
        ChunkPos cp = new ChunkPos(sp);
        return new ChunkCoord(cp.x, cp.z);
    }

    @Override
    public ChunkSnapshot snapshotChunk(DimensionId dim, int chunkX, int chunkZ, boolean forceLoad, BlockMatcher matcher) {
        ServerLevel level = level(dim);
        if (level == null) return null;

        ChunkAccess access = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, forceLoad);
        if (!(access instanceof LevelChunk chunk)) return null;

        boolean biomeEnabled = config.includeBiomeStats();
        LevelChunkSection[] sections = chunk.getSections();
        List<SectionSnapshot> hits = new ArrayList<>();
        int nonAir = 0;
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;
            nonAir++;
            if (!sectionHasMatch(section, matcher)) continue;
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            BiomeId[] biomes = biomeEnabled ? extractBiomes(section.getBiomes()) : null;
            PalettedContainer<BlockState> copy = section.getStates().copy();
            hits.add(new SectionSnapshot(baseY, new NeoForgeSectionPalette(copy), biomes));
        }

        return new ChunkSnapshot(dim, new ChunkCoord(chunkX, chunkZ), hits, nonAir);
    }

    private static boolean sectionHasMatch(LevelChunkSection section, BlockMatcher matcher) {
        boolean[] hit = {false};
        section.getStates().getAll(state -> {
            if (hit[0] || state.isAir()) return;
            ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (rl == null) return;
            if (matcher.matches(new BlockId(rl.getNamespace(), rl.getPath()))) hit[0] = true;
        });
        return hit[0];
    }

    private ServerLevel level(DimensionId dim) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(dim.namespace(), dim.path()));
        return server.getLevel(key);
    }

    private static BiomeId[] extractBiomes(PalettedContainerRO<Holder<Biome>> container) {
        BiomeId[] out = new BiomeId[64];
        for (int qy = 0; qy < 4; qy++) {
            for (int qz = 0; qz < 4; qz++) {
                for (int qx = 0; qx < 4; qx++) {
                    Holder<Biome> h = container.get(qx, qy, qz);
                    ResourceLocation rl = h.unwrapKey().map(k -> k.location()).orElse(null);
                    out[(qy << 4) | (qz << 2) | qx] = rl == null ? null : new BiomeId(rl.getNamespace(), rl.getPath());
                }
            }
        }
        return out;
    }
}
