package com.meryx.backrooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A length of pipe running along one axis, {@code size} pixels across, that you can walk under or squeeze past. */
public class PipeBlock extends RotatedPillarBlock {
    private final VoxelShape alongX;
    private final VoxelShape alongY;
    private final VoxelShape alongZ;

    public PipeBlock(BlockBehaviour.Properties properties, double size) {
        super(properties);
        double lo = 8.0 - size / 2.0;
        double hi = 8.0 + size / 2.0;
        alongX = Block.box(0.0, lo, lo, 16.0, hi, hi);
        alongY = Block.box(lo, 0.0, lo, hi, 16.0, hi);
        alongZ = Block.box(lo, lo, 0.0, hi, hi, 16.0);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> alongX;
            case Y -> alongY;
            case Z -> alongZ;
        };
    }
}
