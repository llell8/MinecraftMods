package com.meryx.backrooms.world;

import com.meryx.backrooms.block.ExitDoorBlock;
import com.meryx.backrooms.block.FluorescentLightBlock;
import com.meryx.backrooms.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The layout of Level 0: a grid of {@link #CELL}-block cells whose edges are randomly walled, with doorways one to
 * three wide and a ceiling light in the middle of each cell. Zones a few cells across change the character of the
 * place: ordinary offices, open halls of pillars, cramped mazes, dark stretches, rooms where the ceiling comes down low
 * or rises high overhead, long parallel hallways, and the pitfalls, an open floor broken by square holes that drop into
 * the dark below.
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
    private static final int LOW_CEILING_Y = 5;
    private static final int TALL_CEILING_Y = 10;
    /** Fall this far below the floor, through a pitfall, and you come out somewhere deeper. */
    public static final int PIT_BOTTOM_Y = -16;

    private static final int ZONE_SIZE = 5;

    private enum Zone { OFFICE, OFFICE2, HALL, CRAMPED, DARK, LOW, TALL, HALLWAYS, PITFALLS }

    private static final double[] ZONE_WEIGHTS = {0.24, 0.16, 0.10, 0.10, 0.08, 0.08, 0.08, 0.08, 0.08};

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
        return TALL_CEILING_Y + 1;
    }

    @Override
    public int cell() {
        return CELL;
    }

    /** The block at a position, or null for air. */
    public static BlockState at(long seed, int x, int y, int z) {
        int cx = Math.floorDiv(x, CELL);
        int cz = Math.floorDiv(z, CELL);
        Zone zone = zone(seed, cx, cz);
        if (y <= FLOOR_Y && zone == Zone.PITFALLS && isPit(seed, x, z)) {
            return null;
        }
        if (y == BOTTOM_Y) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        if (y == FLOOR_Y) {
            return ModBlocks.MOIST_CARPET.defaultBlockState();
        }
        int ceiling = switch (zone) {
            case LOW -> LOW_CEILING_Y;
            case TALL -> TALL_CEILING_Y;
            default -> CEILING_Y;
        };
        if (y == ceiling + 1) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        if (y > ceiling) {
            return null;
        }
        if (y == ceiling) {
            return ceilingAt(seed, x, z, zone);
        }
        return wallAt(seed, x, y, z, zone);
    }

    /** A square hole two blocks across in the pitfalls, never in the middle of a cell. */
    private static boolean isPit(long seed, int x, int z) {
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);
        if (lx >= 3 && lx <= 5 && lz >= 3 && lz <= 5) {
            return false;
        }
        return Math.floorMod(x, 3) < 2 && Math.floorMod(z, 3) < 2
                && random(seed, Math.floorDiv(x, 3), Math.floorDiv(z, 3), 700) < 0.3;
    }

    private static BlockState ceilingAt(long seed, int x, int z, Zone zone) {
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);
        if (lx != CELL / 2 || (lz != CELL / 2 && lz != CELL / 2 - 1)) {
            return ModBlocks.CEILING_TILE.defaultBlockState();
        }
        int cx = Math.floorDiv(x, CELL);
        int cz = Math.floorDiv(z, CELL);
        double deadChance = switch (zone) {
            case DARK -> 0.8;
            case PITFALLS -> 0.45;
            default -> 0.1;
        };
        boolean lit = random(seed, cx, cz, 7) >= deadChance;
        return ModBlocks.FLUORESCENT_LIGHT.defaultBlockState().setValue(FluorescentLightBlock.LIT, lit);
    }

    private static BlockState wallAt(long seed, int x, int y, int z, Zone zone) {
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);
        int cx = Math.floorDiv(x, CELL);
        int cz = Math.floorDiv(z, CELL);
        BlockState wall = ModBlocks.WALLPAPER.defaultBlockState();

        if (lx == 0 && lz == 0) {
            // A corner post: there if the zone has pillars or any wall meets here
            if (zone == Zone.HALL || zone == Zone.TALL
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
        double doorwayChance = zone(seed, cx, cz) == Zone.HALLWAYS ? 0.2 : 0.55;
        if (random(seed, cx, cz, 2 + axis) < doorwayChance) {
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
        Zone zone = zone(seed, cx, cz);
        double chance = switch (zone) {
            case HALL, TALL, PITFALLS -> 0.0;
            case CRAMPED -> 0.8;
            // Walls only one way: long hallways running north-south, side by side
            case HALLWAYS -> axis == 0 ? 1.0 : 0.0;
            default -> 0.5;
        };
        return random(seed, cx, cz, axis) < chance || isExit(seed, cx, cz, axis);
    }

    private static boolean isExit(long seed, int cx, int cz, int axis) {
        Zone zone = zone(seed, cx, cz);
        return zone != Zone.HALL && zone != Zone.TALL && zone != Zone.PITFALLS && random(seed, cx, cz, 10 + axis) < 0.0015;
    }

    private static Zone zone(long seed, int cx, int cz) {
        double r = random(seed, Math.floorDiv(cx, ZONE_SIZE), Math.floorDiv(cz, ZONE_SIZE), 100);
        Zone[] zones = Zone.values();
        double sum = 0;
        for (int i = 0; i < zones.length; i++) {
            sum += ZONE_WEIGHTS[i];
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
