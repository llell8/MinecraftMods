package com.meryx.backrooms.world;

import com.meryx.backrooms.block.ExitDoorBlock;
import com.meryx.backrooms.block.FluorescentLightBlock;
import com.meryx.backrooms.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Level 1, the Habitable Zone: an endless concrete warehouse. Seven blocks of headroom, a 2x2 pillar at every corner of
 * the {@link #CELL}-block grid, the odd concrete wall with a wide opening, damp floors, hanging lights (many dead,
 * whole stretches dark) and stacks of crates.
 */
public final class Level1 implements Layout {
    public static final Level1 INSTANCE = new Level1();
    public static final int CELL = 8;
    private static final int FLOOR_Y = 1;
    private static final int CEILING_Y = 9;

    private Level1() {
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
    public BlockState blockAt(long seed, int x, int y, int z) {
        if (y == 0 || y == CEILING_Y + 1) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        if (y == FLOOR_Y) {
            return ModBlocks.DAMP_CONCRETE.defaultBlockState();
        }
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);
        int cx = Math.floorDiv(x, CELL);
        int cz = Math.floorDiv(z, CELL);
        if (y == CEILING_Y) {
            if (lx == CELL / 2 && (lz == CELL / 2 || lz == CELL / 2 - 1)) {
                boolean dark = Layout.random(seed, Math.floorDiv(cx, 5), Math.floorDiv(cz, 5), 300) < 0.15;
                boolean lit = Layout.random(seed, cx, cz, 301) >= (dark ? 0.85 : 0.25);
                return ModBlocks.FLUORESCENT_LIGHT.defaultBlockState().setValue(FluorescentLightBlock.LIT, lit);
            }
            return ModBlocks.CONCRETE.defaultBlockState();
        }
        // Pillars
        if (lx <= 1 && lz <= 1) {
            return ModBlocks.CONCRETE.defaultBlockState();
        }
        if (lx == 0) {
            return wall(seed, cx, cz, 0, lz, y);
        }
        if (lz == 0) {
            return wall(seed, cx, cz, 1, lx, y);
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
