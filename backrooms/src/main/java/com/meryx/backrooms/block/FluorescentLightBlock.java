package com.meryx.backrooms.block;

import com.meryx.backrooms.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A ceiling light that flickers now and then. Lights that are off and not flickering are dead for good;
 * lights put out while flickering (by a random tick or by a Smiler passing by) come back on by themselves.
 */
public class FluorescentLightBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty FLICKERING = BooleanProperty.create("flickering");

    public FluorescentLightBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, true).setValue(FLICKERING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FLICKERING);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && !state.getValue(FLICKERING) && random.nextInt(3) == 0) {
            flicker(level, pos, state, 2 + random.nextInt(5));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FLICKERING)) {
            return;
        }
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 2 + random.nextInt(5));
        } else if (random.nextInt(5) < 2) {
            // Back on for a moment, then off again
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 1 + random.nextInt(4));
            buzz(level, pos, random);
        } else {
            level.setBlock(pos, state.setValue(LIT, true).setValue(FLICKERING, false), Block.UPDATE_ALL);
        }
    }

    /** Turns a working light off; it comes back on after {@code ticks}. Dead lights are left alone. */
    public static void flicker(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        if (!state.is(ModBlocks.FLUORESCENT_LIGHT) || !state.getValue(LIT)) {
            return;
        }
        level.setBlock(pos, state.setValue(LIT, false).setValue(FLICKERING, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), ticks);
        buzz(level, pos, level.getRandom());
    }

    private static void buzz(Level level, BlockPos pos, RandomSource random) {
        level.playSound(null, pos, ModSounds.LIGHT_BUZZ, SoundSource.BLOCKS, 0.5F, 0.85F + random.nextFloat() * 0.3F);
    }
}
