package com.meryx.backrooms;

import com.meryx.backrooms.block.ModBlocks;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reality wearing thin: now and then a block near a player in the overworld starts glitching, losing its collision,
 * and goes back to normal a minute or so later. Sometimes two glitch one above the other, and a 1x2 glitch is a way
 * in: step into it and you noclip into Level 0.
 */
public final class Glitches {
    /** How often each player gets a chance of a glitch nearby, and the chance. */
    private static final int ROLL_INTERVAL = 20 * 30;
    private static final int ROLL_ONE_IN = 3;
    /** Chance that a glitch takes the block above or below with it, making a way in. */
    private static final float PAIR_CHANCE = 0.4F;
    private static final int RADIUS = 12;

    private record Glitch(ServerLevel level, BlockPos pos, BlockState original, int revertAt) {
    }

    private static final List<Glitch> ACTIVE = new ArrayList<>();

    private Glitches() {
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        revertDue(now, false);

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        for (ServerPlayer player : overworld.players()) {
            if (player.isSpectator()) {
                continue;
            }
            BlockPos feet = player.blockPosition();
            if (overworld.getBlockState(feet).is(ModBlocks.GLITCH) && overworld.getBlockState(feet.above()).is(ModBlocks.GLITCH)) {
                Noclip.queue(player);
            }
            if (now % ROLL_INTERVAL == 0 && overworld.getRandom().nextInt(ROLL_ONE_IN) == 0) {
                glitchNear(overworld, player, now);
            }
        }
    }

    /** Puts every glitched block back, for when the server stops. */
    public static void revertAll() {
        revertDue(Integer.MAX_VALUE, true);
    }

    private static void revertDue(int now, boolean force) {
        for (Iterator<Glitch> it = ACTIVE.iterator(); it.hasNext(); ) {
            Glitch glitch = it.next();
            if (now < glitch.revertAt || (!force && !glitch.level.isLoaded(glitch.pos))) {
                continue;
            }
            if (glitch.level.getBlockState(glitch.pos).is(ModBlocks.GLITCH)) {
                glitch.level.setBlock(glitch.pos, glitch.original, Block.UPDATE_ALL);
            }
            it.remove();
        }
    }

    private static void glitchNear(ServerLevel level, ServerPlayer player, int now) {
        RandomSource random = level.getRandom();
        BlockPos center = player.blockPosition();
        for (int attempt = 0; attempt < 40; attempt++) {
            BlockPos pos = center.offset(random.nextInt(RADIUS * 2 + 1) - RADIUS, random.nextInt(9) - 3, random.nextInt(RADIUS * 2 + 1) - RADIUS);
            if (!canGlitch(level, pos) || !exposed(level, pos)) {
                continue;
            }
            int duration = 20 * (40 + random.nextInt(50));
            glitch(level, pos, now + duration);
            if (random.nextFloat() < PAIR_CHANCE) {
                BlockPos other = random.nextBoolean() ? pos.above() : pos.below();
                if (!canGlitch(level, other)) {
                    other = other.equals(pos.above()) ? pos.below() : pos.above();
                }
                if (canGlitch(level, other)) {
                    glitch(level, other, now + duration);
                }
            }
            level.playSound(null, pos, ModSounds.GLITCH, SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.4F);
            return;
        }
    }

    private static void glitch(ServerLevel level, BlockPos pos, int revertAt) {
        ACTIVE.add(new Glitch(level, pos.immutable(), level.getBlockState(pos), revertAt));
        level.setBlock(pos, ModBlocks.GLITCH.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** Ordinary solid full blocks only: nothing with contents, nothing unbreakable. */
    private static boolean canGlitch(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && !state.is(ModBlocks.GLITCH)
                && !state.hasBlockEntity()
                && state.getFluidState().isEmpty()
                && state.getDestroySpeed(level, pos) >= 0.0F
                && state.isCollisionShapeFullBlock(level, pos);
    }

    /** Glitches are only worth it where someone could see them. */
    private static boolean exposed(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isAir()) {
                return true;
            }
        }
        return false;
    }
}
