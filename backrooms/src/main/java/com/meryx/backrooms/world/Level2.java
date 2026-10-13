package com.meryx.backrooms.world;

import com.meryx.backrooms.block.ExitDoorBlock;
import com.meryx.backrooms.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Level 2, Pipe Dreams: cramped maintenance tunnels three blocks wide and four high, walls and ceiling crowded with
 * pipes, a metal grate underfoot, and only the odd dim red warning light. Each {@link #CELL}-block cell has a small
 * junction in the middle, joined to some of its neighbours by tunnels; everything else is solid.
 */
public final class Level2 implements Layout {
    public static final Level2 INSTANCE = new Level2();
    public static final int CELL = 8;
    private static final int FLOOR_Y = 1;
    private static final int CEILING_Y = 6;
    private static final int LOW = 3;
    private static final int HIGH = 5;

    private Level2() {
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
            return ModBlocks.GRATE_FLOOR.defaultBlockState();
        }
        int lx = Math.floorMod(x, CELL);
        int lz = Math.floorMod(z, CELL);
        int cx = Math.floorDiv(x, CELL);
        int cz = Math.floorDiv(z, CELL);
        if (y == CEILING_Y) {
            if (lx == CELL / 2 && lz == CELL / 2 && Layout.random(seed, cx, cz, 400) < 0.5) {
                return ModBlocks.WARNING_LIGHT.defaultBlockState();
            }
            return ModBlocks.PIPE_CEILING.defaultBlockState();
        }
        if (open(seed, cx, cz, lx, lz)) {
            return null;
        }
        // An exit door, rarely, in the east wall of a junction that has no tunnel that way
        if (lx == HIGH + 1 && lz == CELL / 2 && y <= 3 && !eastOpen(seed, cx, cz) && Layout.random(seed, cx, cz, 410) < 0.012) {
            return ModBlocks.EXIT_DOOR.defaultBlockState().setValue(ExitDoorBlock.HALF, y == 2 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
        }
        return ModBlocks.PIPE_WALL.defaultBlockState();
    }

    private static boolean open(long seed, int cx, int cz, int lx, int lz) {
        boolean midX = lx >= LOW && lx <= HIGH;
        boolean midZ = lz >= LOW && lz <= HIGH;
        if (midX && midZ) {
            return true;
        }
        if (midZ) {
            return lx > HIGH ? eastOpen(seed, cx, cz) : eastOpen(seed, cx - 1, cz);
        }
        if (midX) {
            return lz > HIGH ? southOpen(seed, cx, cz) : southOpen(seed, cx, cz - 1);
        }
        return false;
    }

    private static boolean eastOpen(long seed, int cx, int cz) {
        return Layout.random(seed, cx, cz, 420) < 0.62;
    }

    private static boolean southOpen(long seed, int cx, int cz) {
        return Layout.random(seed, cx, cz, 421) < 0.62;
    }
}
