package com.meryx.backrooms.world;

import com.meryx.backrooms.block.ExitDoorBlock;
import com.meryx.backrooms.block.FluorescentLightBlock;
import com.meryx.backrooms.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The Field: flat grass to the horizon under an ordinary sky, and one house, far off, that you always arrive facing.
 * A dirt path leads up to its door. Inside it is empty but for a flickering light and an exit door in the back wall.
 */
public final class Field implements Layout {
    public static final Field INSTANCE = new Field();
    public static final int GROUND_Y = 4;
    /** Where you arrive, and where the house stands: due north, far enough away to be small on the horizon. */
    public static final int HOUSE_Z = -110;

    private static final int HALF_WIDTH = 5;
    private static final int FRONT_Z = HOUSE_Z + HALF_WIDTH;
    private static final int BACK_Z = HOUSE_Z - HALF_WIDTH;
    private static final int FLOOR_Y = GROUND_Y;
    private static final int WALL_TOP = FLOOR_Y + 5;
    private static final int ROOF_Y = WALL_TOP + 1;

    private Field() {
    }

    @Override
    public int bottomY() {
        return 0;
    }

    @Override
    public int topY() {
        return ROOF_Y + HALF_WIDTH + 4;
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
            // A worn path up to the door
            if (Math.abs(x) <= 1 && z > FRONT_Z && z < FRONT_Z + 40 && Layout.random(seed, x, z, 600) < 0.85 - (z - FRONT_Z) * 0.015) {
                return Blocks.COARSE_DIRT.defaultBlockState();
            }
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (y == GROUND_Y + 1 && Layout.random(seed, x, z, 601) < 0.12 && !(Math.abs(x) <= 1 && z > FRONT_Z)) {
            return Blocks.SHORT_GRASS.defaultBlockState();
        }
        return null;
    }

    /** The house, or null outside it. Walls of pale planks, log corners, glass windows, a stepped roof and a chimney. */
    private static BlockState house(int x, int y, int z) {
        int dx = Math.abs(x);
        int depth = z - HOUSE_Z;
        if (dx > HALF_WIDTH + 1 || Math.abs(depth) > HALF_WIDTH + 1 || y < FLOOR_Y || y > ROOF_Y + HALF_WIDTH + 3) {
            return null;
        }
        // Chimney at the back right, poking through the roof
        if (x == 3 && z == BACK_Z + 2 && y >= ROOF_Y && y <= ROOF_Y + HALF_WIDTH + 3) {
            return Blocks.BRICKS.defaultBlockState();
        }
        if (y >= ROOF_Y) {
            // Stepped roof: each layer one smaller, overhanging the walls by one
            int layer = y - ROOF_Y;
            int reach = HALF_WIDTH + 1 - layer;
            if (reach >= 0 && dx <= reach && Math.abs(depth) <= reach && (dx == reach || Math.abs(depth) == reach || layer == 0)) {
                return Blocks.DARK_OAK_PLANKS.defaultBlockState();
            }
            return null;
        }
        if (dx > HALF_WIDTH || Math.abs(depth) > HALF_WIDTH) {
            return null;
        }
        boolean wall = dx == HALF_WIDTH || Math.abs(depth) == HALF_WIDTH;
        if (y == FLOOR_Y) {
            return (wall ? Blocks.COBBLESTONE : Blocks.SPRUCE_PLANKS).defaultBlockState();
        }
        if (!wall) {
            // Empty inside, but for one light that will not stop flickering
            if (y == WALL_TOP && x == 0 && z == HOUSE_Z) {
                return ModBlocks.FLUORESCENT_LIGHT.defaultBlockState().setValue(FluorescentLightBlock.LIT, true);
            }
            return y == WALL_TOP ? Blocks.SPRUCE_PLANKS.defaultBlockState() : null;
        }
        // Front door
        if (z == FRONT_Z && x == 0 && y <= FLOOR_Y + 2) {
            return Blocks.OAK_DOOR.defaultBlockState()
                    .setValue(DoorBlock.FACING, Direction.SOUTH)
                    .setValue(DoorBlock.HALF, y == FLOOR_Y + 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
        }
        // The way out, in the back wall
        if (z == BACK_Z && x == 0 && y <= FLOOR_Y + 2) {
            return ModBlocks.EXIT_DOOR.defaultBlockState()
                    .setValue(ExitDoorBlock.HALF, y == FLOOR_Y + 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
        }
        if (dx == HALF_WIDTH && Math.abs(depth) == HALF_WIDTH) {
            return Blocks.STRIPPED_OAK_LOG.defaultBlockState();
        }
        // Windows either side of the door and in the side walls
        int along = Math.abs(depth) == HALF_WIDTH ? dx : Math.abs(depth);
        if ((y == FLOOR_Y + 2 || y == FLOOR_Y + 3) && along >= 2 && along <= 3 && z != BACK_Z) {
            return Blocks.GLASS.defaultBlockState();
        }
        return Blocks.BIRCH_PLANKS.defaultBlockState();
    }
}
