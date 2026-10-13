package com.meryx.backrooms.world;

import com.meryx.backrooms.block.ExitDoorBlock;
import com.meryx.backrooms.block.FluorescentLightBlock;
import com.meryx.backrooms.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The Field: perfectly flat, bright green ground under an ordinary sky, and one big white two-storey house far off to
 * the north, which you always arrive facing. A path leads up to its porch. Inside, a staircase climbs to an empty upper
 * floor, and the way out is a door in the back wall up there.
 */
public final class Field implements Layout {
    public static final Field INSTANCE = new Field();
    public static final int GROUND_Y = 4;
    /** Where the house stands: due north of where you arrive, far enough to sit small on the horizon. */
    public static final int HOUSE_Z = -110;

    private static final int HALF_W = 7;
    private static final int HALF_D = 5;
    private static final int FRONT_Z = HOUSE_Z + HALF_D;
    private static final int BACK_Z = HOUSE_Z - HALF_D;
    private static final int FLOOR1 = GROUND_Y + 1;
    private static final int FLOOR2 = FLOOR1 + 4;
    private static final int ATTIC = FLOOR2 + 4;
    private static final int ROOF = ATTIC + 1;
    private static final int PORCH_DEPTH = 3;
    private static final int STAIR_Z = BACK_Z + 2;

    private Field() {
    }

    @Override
    public int bottomY() {
        return 0;
    }

    @Override
    public int topY() {
        return ROOF + HALF_D + 4;
    }

    @Override
    public int cell() {
        return 16;
    }

    @Override
    public BlockState blockAt(long seed, int x, int y, int z) {
        BlockState house = house(x, y, z);
        if (house != null) {
            return house;
        }
        if (y == 0) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        if (y < GROUND_Y) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (y == GROUND_Y) {
            // A worn path from the porch steps
            if (Math.abs(x) <= 1 && z > FRONT_Z + PORCH_DEPTH && z < FRONT_Z + 45
                    && Layout.random(seed, x, z, 600) < 0.9 - (z - FRONT_Z) * 0.015) {
                return Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            }
            return Blocks.LIME_WOOL.defaultBlockState();
        }
        return null;
    }

    private static BlockState house(int x, int y, int z) {
        int dx = Math.abs(x);
        int depth = z - HOUSE_Z;
        if (dx > HALF_W + 1 || z < BACK_Z - 1 || z > FRONT_Z + PORCH_DEPTH + 1 || y < FLOOR1 || y > ROOF + HALF_D + 3) {
            return null;
        }
        BlockState porch = porch(x, y, z, dx);
        if (porch != null) {
            return porch;
        }
        if (z > FRONT_Z + 1) {
            return null;
        }
        // Brick chimney up the east end, standing proud of the roof
        if (x == HALF_W - 2 && depth == -1 && y >= ATTIC && y <= ROOF + HALF_D + 3) {
            return Blocks.BRICKS.defaultBlockState();
        }
        if (y >= ROOF) {
            return roof(dx, depth, y - ROOF);
        }
        if (dx > HALF_W || Math.abs(depth) > HALF_D) {
            return null;
        }
        boolean wall = dx == HALF_W || Math.abs(depth) == HALF_D;
        if (y == FLOOR1) {
            return (wall ? Blocks.STONE_BRICKS : Blocks.OAK_PLANKS).defaultBlockState();
        }
        if (!wall) {
            return inside(x, y, z);
        }
        // Corner boards
        if (dx == HALF_W && Math.abs(depth) == HALF_D) {
            return Blocks.QUARTZ_PILLAR.defaultBlockState();
        }
        // A band of trim between the floors
        if (y == FLOOR2) {
            return Blocks.SMOOTH_QUARTZ.defaultBlockState();
        }
        // Front door
        if (z == FRONT_Z && x == 0 && y <= FLOOR1 + 2) {
            return Blocks.SPRUCE_DOOR.defaultBlockState()
                    .setValue(DoorBlock.FACING, Direction.SOUTH)
                    .setValue(DoorBlock.HALF, y == FLOOR1 + 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
        }
        // The way out: a door in the back wall upstairs
        if (z == BACK_Z && x == 0 && y > FLOOR2 && y <= FLOOR2 + 2) {
            return ModBlocks.EXIT_DOOR.defaultBlockState()
                    .setValue(ExitDoorBlock.HALF, y == FLOOR2 + 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
        }
        // Windows: two wide windows on the front and back of each floor with dark shutters, one on each side
        boolean frontOrBack = Math.abs(depth) == HALF_D;
        int along = frontOrBack ? dx : Math.abs(depth);
        int sill = y > FLOOR2 ? FLOOR2 + 2 : FLOOR1 + 2;
        boolean windowRow = y == sill || y == sill + 1;
        if (windowRow && !(z == BACK_Z && y > FLOOR2 && x == 0)) {
            boolean glass = frontOrBack ? along == 2 || along == 3 || along == 5 || along == 6 : along == 2;
            if (glass) {
                return frontOrBack
                        ? Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true)
                        : Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true);
            }
            if (frontOrBack && (along == 1 || along == 4)) {
                return Blocks.GRAY_CONCRETE.defaultBlockState();
            }
        }
        return Blocks.WHITE_CONCRETE.defaultBlockState();
    }

    /** A covered porch along the front, white posts, a railing and three steps down to the path. */
    private static BlockState porch(int x, int y, int z, int dx) {
        if (z <= FRONT_Z || z > FRONT_Z + PORCH_DEPTH + 1 || dx > HALF_W) {
            return null;
        }
        int out = z - FRONT_Z;
        if (out == PORCH_DEPTH + 1) {
            // Steps down to the path
            return y == FLOOR1 && dx <= 1 ? Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH) : null;
        }
        if (y == FLOOR1) {
            return Blocks.SPRUCE_PLANKS.defaultBlockState();
        }
        boolean post = out == PORCH_DEPTH && (dx == HALF_W || dx == 2 || dx == 5);
        if (y > FLOOR1 && y < FLOOR2) {
            if (post) {
                return Blocks.QUARTZ_PILLAR.defaultBlockState();
            }
            if (y == FLOOR1 + 1 && (out == PORCH_DEPTH || dx == HALF_W) && dx > 1) {
                return Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState()
                        .setValue(IronBarsBlock.EAST, out == PORCH_DEPTH).setValue(IronBarsBlock.WEST, out == PORCH_DEPTH)
                        .setValue(IronBarsBlock.NORTH, dx == HALF_W).setValue(IronBarsBlock.SOUTH, dx == HALF_W);
            }
            return null;
        }
        if (y == FLOOR2) {
            return Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState();
        }
        return null;
    }

    /** A gable roof of dark shingles, the ridge running east to west, white gable ends. */
    private static BlockState roof(int dx, int depth, int layer) {
        int reach = HALF_D + 1 - layer;
        if (reach < 0 || dx > HALF_W + 1 || Math.abs(depth) > reach) {
            return null;
        }
        if (reach == 0) {
            return Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        if (Math.abs(depth) == reach) {
            return Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, depth > 0 ? Direction.NORTH : Direction.SOUTH);
        }
        if (dx == HALF_W) {
            return Blocks.WHITE_CONCRETE.defaultBlockState();
        }
        return null;
    }

    /** Empty rooms, a staircase along the back wall, a floor between them with a hole for the stairs, one light. */
    private static BlockState inside(int x, int y, int z) {
        // Stairs climbing east along the back wall, from the ground floor to the upper floor
        int step = x + 5;
        if (z == STAIR_Z && step >= 0 && step <= 3 && y == FLOOR1 + 1 + step) {
            return Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        }
        if (y == FLOOR2) {
            if (z == STAIR_Z && x >= -5 && x <= -2) {
                return null;
            }
            if (x == 0 && z == HOUSE_Z) {
                return ModBlocks.FLUORESCENT_LIGHT.defaultBlockState().setValue(FluorescentLightBlock.LIT, true);
            }
            return Blocks.OAK_PLANKS.defaultBlockState();
        }
        if (y == ATTIC) {
            return Blocks.OAK_PLANKS.defaultBlockState();
        }
        return null;
    }
}
