package com.meryx.backrooms.world;

import com.meryx.backrooms.Backrooms;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * Generates one level of the Backrooms from its {@link Layout}: Level 0's {@link Maze}, Level 1's {@link Level1}, or
 * Level 2's {@link Level2}. No terrain, caves, decorations or mobs. Each level has its own generator type,
 * {@code backrooms:level_0} and so on.
 */
public class BackroomsChunkGenerator extends ChunkGenerator {
    private static final Layout[] LAYOUTS = {Maze.INSTANCE, Level1.INSTANCE, Level2.INSTANCE};
    public static final List<MapCodec<BackroomsChunkGenerator>> CODECS = List.of(codecFor(0), codecFor(1), codecFor(2));

    private final int level;
    private final Layout layout;

    public BackroomsChunkGenerator(BiomeSource biomeSource, int level) {
        super(biomeSource);
        this.level = level;
        this.layout = LAYOUTS[level];
    }

    private static MapCodec<BackroomsChunkGenerator> codecFor(int level) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
        ).apply(instance, biomeSource -> new BackroomsChunkGenerator(biomeSource, level)));
    }

    /** The maze layout is seeded by the world seed. */
    private static long seed(RandomState randomState) {
        // Level 0's seed is kept as it was, so existing worlds keep their maze
        return randomState.getOrCreateRandomFactory(Backrooms.id("level_0")).at(0, 0, 0).nextLong();
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODECS.get(level);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        long seed = seed(randomState);
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int y = layout.bottomY(); y <= layout.topY(); y++) {
            LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockState state = layout.blockAt(seed, minX + x, y, minZ + z);
                    if (state != null) {
                        section.setBlockState(x, y & 15, z, state, false);
                        oceanFloor.update(x, y, z, state);
                        worldSurface.update(x, y, z, state);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return layout.topY() + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        long seed = seed(randomState);
        BlockState[] states = new BlockState[level.getHeight()];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        for (int y = layout.bottomY(); y <= layout.topY(); y++) {
            BlockState state = layout.blockAt(seed, x, y, z);
            if (state != null) {
                states[y - level.getMinY()] = state;
            }
        }
        return new NoiseColumn(level.getMinY(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
    }
}
