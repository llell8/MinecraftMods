package com.meryx.backrooms.world;

import com.meryx.backrooms.block.ExitDoorBlock;
import com.meryx.backrooms.block.FluorescentLightBlock;
import com.meryx.backrooms.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The layout of Level 0: a grid of {@link #CELL}-block cells whose edges are randomly walled, with doorway gaps,
 * a ceiling light in the middle of each cell, and zones that are open pillar halls, cramped corridors or dark.
 * Everything is a pure function of the world seed and the block position, so chunks line up wherever they generate.
 */
public final class Maze {
    public static final int CELL = 8;
    public static final int BOTTOM_Y = 0;
    public static final int FLOOR_Y = 1;
    /** The first air block above the floor, where players and Smilers stand. */
    public static final int STAND_Y = 2;
    public static final int CEILING_Y = 6;
    public static final int TOP_Y = 7;

    private static final int ZONE_SIZE = 5;

    private enum Zone { OFFICE, OFFICE2, HALL, CRAMPED, DARK }

    private Maze() {
    }

    /** The block at a position, or null for air. */
    public static BlockState blockAt(long seed, int x, int y, int z) {
        if (y == BOTTOM_Y || y == TOP_Y) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        if (y == FLOOR_Y) {
            return ModBlocks.MOIST_CARPET.defaultBlockState();
        }
        if (y == CEILING_Y) {
            return ceilingAt(seed, x, z);
        }
        if (y > FLOOR_Y && y < CEILING_Y) {
            return wallAt(seed, x, y, z);
        }
        return null;
    }

    private static BlockState ceilingAt(long seed, int x, int z) {
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);
        if (lx != CELL / 2 || (lz != CELL / 2 && lz != CELL / 2 - 1)) {
            return ModBlocks.CEILING_TILE.defaultBlockState();
        }
        int cx = Math.floorDiv(x, CELL);
        int cz = Math.floorDiv(z, CELL);
        double deadChance = zone(seed, cx, cz) == Zone.DARK ? 0.8 : 0.1;
        boolean lit = random(seed, cx, cz, 7) >= deadChance;
        return ModBlocks.FLUORESCENT_LIGHT.defaultBlockState().setValue(FluorescentLightBlock.LIT, lit);
    }

    private static BlockState wallAt(long seed, int x, int y, int z) {
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);
        int cx = Math.floorDiv(x, CELL);
        int cz = Math.floorDiv(z, CELL);
        BlockState wall = ModBlocks.WALLPAPER.defaultBlockState();

        if (lx == 0 && lz == 0) {
            // A corner post: there if the zone has pillars or any wall meets here
            if (zone(seed, cx, cz) == Zone.HALL
                    || hasWall(seed, cx, cz, 0) || hasWall(seed, cx, cz - 1, 0)
                    || hasWall(seed, cx, cz, 1) || hasWall(seed, cx - 1, cz, 1)) {
                return wall;
            }
            return null;
        }
        if (lx == 0) {
            return edgeBlock(seed, cx, cz, 0, lz, y, wall);
        }
        if (lz == 0) {
            return edgeBlock(seed, cx, cz, 1, lx, y, wall);
        }
        return null;
    }

    /** One block of the wall on edge {@code axis} of cell (cx, cz), {@code along} blocks along it. */
    private static BlockState edgeBlock(long seed, int cx, int cz, int axis, int along, int y, BlockState wall) {
        if (!hasWall(seed, cx, cz, axis)) {
            return null;
        }
        if (isExit(seed, cx, cz, axis)) {
            if (along == CELL / 2 && y <= STAND_Y + 1) {
                return ModBlocks.EXIT_DOOR.defaultBlockState()
                        .setValue(ExitDoorBlock.HALF, y == STAND_Y ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
            }
            return wall;
        }
        if (random(seed, cx, cz, 2 + axis) < 0.55) {
            // A doorway: a two-wide gap, three blocks tall
            int gap = 1 + (int) (random(seed, cx, cz, 4 + axis) * (CELL - 3));
            if ((along == gap || along == gap + 1) && y <= STAND_Y + 2) {
                return null;
            }
        }
        return wall;
    }

    private static boolean hasWall(long seed, int cx, int cz, int axis) {
        double chance = switch (zone(seed, cx, cz)) {
            case HALL -> 0.0;
            case CRAMPED -> 0.8;
            default -> 0.5;
        };
        return random(seed, cx, cz, axis) < chance || isExit(seed, cx, cz, axis);
    }

    private static boolean isExit(long seed, int cx, int cz, int axis) {
        return zone(seed, cx, cz) != Zone.HALL && random(seed, cx, cz, 10 + axis) < 0.0015;
    }

    private static Zone zone(long seed, int cx, int cz) {
        double r = random(seed, Math.floorDiv(cx, ZONE_SIZE), Math.floorDiv(cz, ZONE_SIZE), 100);
        Zone[] zones = Zone.values();
        // Offices are the most common; halls, cramped mazes and dark zones break them up
        double[] weights = {0.35, 0.25, 0.15, 0.15, 0.10};
        double sum = 0;
        for (int i = 0; i < zones.length; i++) {
            sum += weights[i];
            if (r < sum) {
                return zones[i];
            }
        }
        return Zone.OFFICE;
    }

    /** A random number in [0, 1) for a cell and a salt. */
    private static double random(long seed, int cx, int cz, int salt) {
        long h = seed ^ (cx * 0x9E3779B97F4A7C15L) ^ (cz * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }
}
