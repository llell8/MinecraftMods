package com.meryx.backrooms.world;

import com.meryx.backrooms.block.ExitDoorBlock;
import com.meryx.backrooms.block.FluorescentLightBlock;
import com.meryx.backrooms.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Level 2, Pipe Dreams: one low basement after another, painted cinderblock walls with doorways between them, stained
 * concrete floors with drains and puddles, and pipes everywhere. Big mains run along the ceiling for several rooms at a
 * time, straight through the walls; pairs of small pipes run beside them; more pipes hug the walls at head height, and
 * risers climb the corners. The lights are a bare few: dead or flickering fluorescents, and red warning lamps.
 */
public final class Level2 implements Layout {
    public static final Level2 INSTANCE = new Level2();
    public static final int CELL = 10;
    private static final int FLOOR_Y = 1;
    private static final int CEILING_Y = 6;

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
        int mid = CELL / 2;
        if (y == FLOOR_Y) {
            if (lx == mid && lz == mid && Layout.random(seed, cx, cz, 500) < 0.4) {
                return ModBlocks.FLOOR_DRAIN.defaultBlockState();
            }
            // Puddles where water has gathered
            return (Layout.random(seed, Math.floorDiv(x, 3), Math.floorDiv(z, 3), 501) < 0.12 ? ModBlocks.DAMP_CONCRETE
                    : ModBlocks.BASEMENT_FLOOR).defaultBlockState();
        }
        if (y == CEILING_Y) {
            if (lx == mid && lz == mid) {
                double kind = Layout.random(seed, cx, cz, 502);
                if (kind < 0.35) {
                    return ModBlocks.FLUORESCENT_LIGHT.defaultBlockState()
                            .setValue(FluorescentLightBlock.LIT, Layout.random(seed, cx, cz, 503) < 0.5);
                }
                if (kind < 0.65) {
                    return ModBlocks.WARNING_LIGHT.defaultBlockState();
                }
            }
            return ModBlocks.CONCRETE.defaultBlockState();
        }

        // Walls between basements, with a doorway in most
        if (lx == 0 || lz == 0) {
            BlockState wall = wall(seed, cx, cz, lx, lz, y);
            if (wall != null) {
                return wall;
            }
        }
        return pipes(seed, x, y, z, lx, lz, cx, cz);
    }

    private static BlockState wall(long seed, int cx, int cz, int lx, int lz, int y) {
        BlockState block = ModBlocks.CINDERBLOCK.defaultBlockState();
        if (lx == 0 && lz == 0) {
            return block;
        }
        int axis = lx == 0 ? 0 : 1;
        int along = lx == 0 ? lz : lx;
        boolean doorway = Layout.random(seed, cx, cz, 510 + axis) < 0.75;
        if (doorway) {
            return along >= 4 && along <= 5 && y <= FLOOR_Y + 3 ? null : block;
        }
        if (along == 5 && y <= FLOOR_Y + 2 && Layout.random(seed, cx, cz, 520 + axis) < 0.012) {
            return ModBlocks.EXIT_DOOR.defaultBlockState()
                    .setValue(ExitDoorBlock.HALF, y == FLOOR_Y + 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
        }
        return block;
    }

    private static BlockState pipes(long seed, int x, int y, int z, int lx, int lz, int cx, int cz) {
        int top = CEILING_Y - 1;
        // Risers climbing a corner of some rooms
        boolean riser = (lx == 1 && lz == 1 && Layout.random(seed, cx, cz, 530) < 0.45)
                || (lx == CELL - 1 && lz == CELL - 1 && Layout.random(seed, cx, cz, 531) < 0.35);
        if (riser) {
            return pipe(lx == 1 ? ModBlocks.PIPE.defaultBlockState() : ModBlocks.SMALL_PIPE.defaultBlockState(), Direction.Axis.Y);
        }
        // Mains along the ceiling, running east-west for several rooms at a time, with two small pipes beside them
        int stretch = Math.floorDiv(x, CELL * 4);
        int main = 2 + (int) (Layout.random(seed, 0, cz, 540) * 3);
        if (y == top && Layout.random(seed, stretch, cz, 541) < 0.75) {
            if (lz == main) {
                return pipe(ModBlocks.PIPE.defaultBlockState(), Direction.Axis.X);
            }
            if (lz == main + 2 || lz == main + 3) {
                return pipe(ModBlocks.SMALL_PIPE.defaultBlockState(), Direction.Axis.X);
            }
        }
        // Small pipes running north-south along the walls at head height
        int stretchZ = Math.floorDiv(z, CELL * 3);
        if (y == top - 1 && (lx == 1 || lx == CELL - 1) && Layout.random(seed, cx * 2 + (lx == 1 ? 0 : 1), stretchZ, 550) < 0.55) {
            return pipe(ModBlocks.SMALL_PIPE.defaultBlockState(), Direction.Axis.Z);
        }
        // Now and then a big pipe crossing north-south under the mains
        if (y == top - 1 && lx == 7 && Layout.random(seed, cx, stretchZ, 560) < 0.3) {
            return pipe(ModBlocks.PIPE.defaultBlockState(), Direction.Axis.Z);
        }
        return null;
    }

    private static BlockState pipe(BlockState state, Direction.Axis axis) {
        return state.setValue(RotatedPillarBlock.AXIS, axis);
    }
}
