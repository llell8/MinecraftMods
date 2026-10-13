package com.meryx.backrooms.world;

import com.meryx.backrooms.block.ExitDoorBlock;
import com.meryx.backrooms.block.FluorescentLightBlock;
import com.meryx.backrooms.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Level 1, the Habitable Zone: an endless concrete warehouse. Seven blocks of headroom, a 2x2 pillar at every corner of
 * the {@link #CELL}-block grid (hazard-striped at the foot), hanging lights, and zones that change as you walk: rooms
 * split by the odd concrete wall with crates in the corners, open halls with nothing but pillars, storage with rows of
 * crates stacked high, flooded stretches where the floor is wet all over, and dark ones where most lights are dead.
 */
public final class Level1 implements Layout {
    public static final Level1 INSTANCE = new Level1();
    public static final int CELL = 8;
    private static final int FLOOR_Y = 1;
    private static final int CEILING_Y = 9;

    private enum Zone { NORMAL, HALL, STORAGE, FLOODED, DARK }

    private Level1() {
    }

    private static Zone zone(long seed, int cx, int cz) {
        double r = Layout.random(seed, Math.floorDiv(cx, 5), Math.floorDiv(cz, 5), 300);
        if (r < 0.40) return Zone.NORMAL;
        if (r < 0.58) return Zone.HALL;
        if (r < 0.75) return Zone.STORAGE;
        if (r < 0.88) return Zone.FLOODED;
        return Zone.DARK;
    }

    @Override
    public int bottomY() {
        return 0;
    }

    @Override
    public int topY() {
        return CEILING_Y + 1;
    }

    @Override
    public int cell() {
        return CELL;
    }

    @Override
    public BlockState blockAt(long seed, int x, int y, int z) {
        if (y == 0 || y == CEILING_Y + 1) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);
        int cx = Math.floorDiv(x, CELL);
        int cz = Math.floorDiv(z, CELL);
        Zone zone = zone(seed, cx, cz);
        if (y == FLOOR_Y) {
            // Bare concrete, with damp patches; flooded zones are wet all over
            boolean wet = zone == Zone.FLOODED || Layout.random(seed, Math.floorDiv(x, 4), Math.floorDiv(z, 4), 302) < 0.3;
            return (wet ? ModBlocks.DAMP_CONCRETE : ModBlocks.CONCRETE).defaultBlockState();
        }
        if (y == CEILING_Y) {
            if (lx == CELL / 2 && (lz == CELL / 2 || lz == CELL / 2 - 1)) {
                boolean lit = Layout.random(seed, cx, cz, 301) >= (zone == Zone.DARK ? 0.85 : 0.25);
                return ModBlocks.FLUORESCENT_LIGHT.defaultBlockState().setValue(FluorescentLightBlock.LIT, lit);
            }
            return ModBlocks.CONCRETE.defaultBlockState();
        }
        // Pillars, with a band of hazard paint at the bottom
        if (lx <= 1 && lz <= 1) {
            return (y == FLOOR_Y + 1 ? ModBlocks.HAZARD_CONCRETE : ModBlocks.CONCRETE).defaultBlockState();
        }
        if (zone != Zone.HALL && lx == 0) {
            return wall(seed, cx, cz, 0, lz, y);
        }
        if (zone != Zone.HALL && lz == 0) {
            return wall(seed, cx, cz, 1, lx, y);
        }
        if (zone == Zone.STORAGE) {
            // Rows of crates stacked high, with aisles between them and the middle of the cell kept clear
            if ((lz == 2 || lz == 6) && lx >= 2 && lx <= 7 && lx != 4) {
                int height = 1 + (int) (Layout.random(seed, cx * 13 + lx, cz * 7 + lz, 313) * 4);
                return y - FLOOR_Y <= height ? ModBlocks.CRATE.defaultBlockState() : null;
            }
            return null;
        }
        // A stack of crates in one corner of some cells; the middle of every cell stays clear
        if (lx >= 5 && lz >= 6 && Layout.random(seed, cx, cz, 310) < 0.3) {
            int height = 1 + (int) (Layout.random(seed, cx, cz, 311 + lx * 7 + lz) * 3);
            if (y - FLOOR_Y <= height && Layout.random(seed, cx * 31 + lx, cz * 17 + lz, 312) < 0.7) {
                return ModBlocks.CRATE.defaultBlockState();
            }
        }
        return null;
    }

    private static BlockState wall(long seed, int cx, int cz, int axis, int along, int y) {
        if (Layout.random(seed, cx, cz, 320 + axis) >= 0.25) {
            return null;
        }
        if (Layout.random(seed, cx, cz, 330 + axis) < 0.004) {
            if (along == CELL / 2 && y <= 3) {
                return ModBlocks.EXIT_DOOR.defaultBlockState().setValue(ExitDoorBlock.HALF, y == 2 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
            }
            return ModBlocks.CONCRETE.defaultBlockState();
        }
        if (Layout.random(seed, cx, cz, 340 + axis) < 0.6 && along >= 3 && along <= 5 && y <= 4) {
            return null;
        }
        return ModBlocks.CONCRETE.defaultBlockState();
    }
}
