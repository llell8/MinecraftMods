package com.meryx.backrooms.world;

import net.minecraft.world.level.block.state.BlockState;

/** The shape of one level of the Backrooms: a pure function of the world seed and the block position. */
public interface Layout {
    /** The block at a position, or null for air. */
    BlockState blockAt(long seed, int x, int y, int z);

    /** The lowest and highest y the layout ever puts a block at. */
    int bottomY();

    int topY();

    /** The size of the grid the level is laid out on. The middle of every cell is always open floor. */
    int cell();

    /** The middle of the cell a position is in, where it is safe to put someone. */
    default int cellCenter(int coordinate) {
        return Math.floorDiv(coordinate, cell()) * cell() + cell() / 2;
    }

    /** A random number in [0, 1) for a cell and a salt. */
    static double random(long seed, int cx, int cz, int salt) {
        long h = seed ^ (cx * 0x9E3779B97F4A7C15L) ^ (cz * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }
}
