package com.meryx.backrooms.world;

import com.meryx.backrooms.block.ExitDoorBlock;
import com.meryx.backrooms.block.FluorescentLightBlock;
import com.meryx.backrooms.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The layout of Level 0: a grid of {@link #CELL}-block cells whose edges are randomly walled, with doorway gaps,
 * a ceiling light in the middle of each cell, lone pillars and old cubicle partitions, and zones that are open pillar
 * halls, cramped corridors, dark, or under a lower ceiling.
 * Everything is a pure function of the world seed and the block position, so chunks line up wherever they generate.
 */
public final class Maze implements Layout {
    public static final Maze INSTANCE = new Maze();

    public static final int CELL = 8;
    public static final int BOTTOM_Y = 0;
    public static final int FLOOR_Y = 1;
    /** The first air block above the floor, where players and Smilers stand. */
    public static final int STAND_Y = 2;
    public static final int CEILING_Y = 6;
    public static final int TOP_Y = 7;

    private static final int ZONE_SIZE = 5;

    private enum Zone { OFFICE, OFFICE2, HALL, CRAMPED, DARK, LOW }

    private Maze() {
    }

    @Override
    public BlockState blockAt(long seed, int x, int y, int z) {
        return at(seed, x, y, z);
    }

    @Override
    public int bottomY() {
        return BOTTOM_Y;
    }

    @Override
    public int topY() {
        return TOP_Y;
    }

    @Override
    public int cell() {
        return CELL;
    }

    /** The block at a position, or null for air. */
    public static BlockState at(long seed, int x, int y, int z) {
        if (y == BOTTOM_Y || y == TOP_Y) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        if (y == FLOOR_Y) {
            return ModBlocks.MOIST_CARPET.defaultBlockState();
        }
        // In low zones the ceiling comes down a block, close over your head
        int ceiling = zone(seed, Math.floorDiv(x, CELL), Math.floorDiv(z, CELL)) == Zone.LOW ? CEILING_Y - 1 : CEILING_Y;
        if (y == ceiling) {
            return ceilingAt(seed, x, z);
        }
        if (y > ceiling) {
            return ModBlocks.CEILING_TILE.defaultBlockState();
        }
        if (y > FLOOR_Y) {
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
        return furniture(seed, cx, cz, lx, lz, y, wall);
    }

    /**
     * Things inside a room, kept clear of its middle: a lone pillar, or in offices the waist-high partitions of
     * cubicles that are long gone.
     */
    private static BlockState furniture(long seed, int cx, int cz, int lx, int lz, int y, BlockState wall) {
        Zone zone = zone(seed, cx, cz);
        if (zone == Zone.HALL || zone == Zone.CRAMPED) {
            return null;
        }
        if (random(seed, cx, cz, 20) < 0.12) {
            int px = random(seed, cx, cz, 21) < 0.5 ? 2 : 6;
            int pz = random(seed, cx, cz, 22) < 0.5 ? 2 : 6;
            if (lx == px && lz == pz) {
                return wall;
            }
        }
        if ((zone == Zone.OFFICE || zone == Zone.OFFICE2) && y == STAND_Y && random(seed, cx, cz, 23) < 0.25) {
            // An L of partition: along one side of the room and round the corner
            boolean along = lz == 2 && lx >= 2 && lx <= 5;
            boolean down = lx == 2 && lz >= 2 && lz <= 3;
            if (along || down) {
                return wall;
            }
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
            // A doorway, one to three wide, three blocks tall (two where the ceiling is low)
            int width = 1 + (int) (random(seed, cx, cz, 6 + axis) * 3);
            int gap = 1 + (int) (random(seed, cx, cz, 4 + axis) * (CELL - 1 - width));
            if (along >= gap && along < gap + width && y <= STAND_Y + 2) {
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
        // Offices are the most common; halls, cramped mazes, dark zones and low ceilings break them up
        double[] weights = {0.30, 0.22, 0.14, 0.14, 0.10, 0.10};
        double sum = 0;
        for (int i = 0; i < zones.length; i++) {
            sum += weights[i];
            if (r < sum) {
                return zones[i];
            }
        }
        return Zone.OFFICE;
    }

    private static double random(long seed, int cx, int cz, int salt) {
        return Layout.random(seed, cx, cz, salt);
    }
}
