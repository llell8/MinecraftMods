package com.meryx.backrooms;

import com.meryx.backrooms.block.ModBlocks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Reality wearing thin. At any moment a block near a player can start glitching: a see-through, flickering overlay of
 * static and torn wallpaper appears over it, then fades a little later. The overlay is a vanilla block display showing
 * the animated {@code backrooms:glitch} block, a hair bigger than the block it covers.
 * <p>
 * Sometimes two blocks one above the other glitch together. Those tear open: the blocks vanish, leaving a 1x2 doorway
 * of static. Step into it and you noclip into Level 0. The blocks come back when the glitch ends.
 */
public final class Glitches {
    /** Every display this mod spawns carries this tag, so leftovers from a crash can be cleaned up. */
    public static final String TAG = "backrooms_glitch";

    /** How often each player gets a chance of a glitch nearby, and the chance. */
    private static final int ROLL_INTERVAL = 100;
    private static final int ROLL_ONE_IN = 5;
    /** Chance that a glitch is a 1x2 tear, a way in. */
    private static final float PAIR_CHANCE = 0.3F;
    private static final int RADIUS = 12;

    private record Glitch(ServerLevel level, BlockPos pos, BlockState original, boolean torn, String tag, int endAt) {
    }

    private static final List<Glitch> ACTIVE = new ArrayList<>();
    private static final Set<String> ACTIVE_TAGS = new HashSet<>();
    private static int nextId;

    private Glitches() {
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        endDue(now, false);

        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().equals(Backrooms.LEVEL_0)) {
                continue;
            }
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator()) {
                    continue;
                }
                if (inDoorway(level, player.blockPosition())) {
                    Noclip.queue(player);
                    continue;
                }
                if (now % ROLL_INTERVAL == 0 && level.getRandom().nextInt(ROLL_ONE_IN) == 0) {
                    glitchNear(level, player, now);
                }
            }
        }
    }

    /** Feet and head both inside a torn glitch, or inside placed glitch blocks (handy for testing). */
    private static boolean inDoorway(ServerLevel level, BlockPos feet) {
        if (level.getBlockState(feet).is(ModBlocks.GLITCH) && level.getBlockState(feet.above()).is(ModBlocks.GLITCH)) {
            return true;
        }
        boolean lower = false;
        boolean upper = false;
        for (Glitch glitch : ACTIVE) {
            if (glitch.torn && glitch.level == level) {
                lower |= glitch.pos.equals(feet);
                upper |= glitch.pos.equals(feet.above());
            }
        }
        return lower && upper;
    }

    /** Ends every glitch and puts torn blocks back, for when the server stops. */
    public static void endAll() {
        endDue(Integer.MAX_VALUE, true);
    }

    /** Glitch displays left over from a crash are not tracked any more: get rid of them as they load. */
    public static void onEntityLoad(Entity entity) {
        if (entity.getTags().contains(TAG) && entity.getTags().stream().noneMatch(ACTIVE_TAGS::contains)) {
            entity.discard();
        }
    }

    private static void endDue(int now, boolean force) {
        for (Iterator<Glitch> it = ACTIVE.iterator(); it.hasNext(); ) {
            Glitch glitch = it.next();
            if (now < glitch.endAt || (!force && !glitch.level.isLoaded(glitch.pos))) {
                continue;
            }
            for (Entity display : glitch.level.getEntities((Entity) null, new AABB(glitch.pos).inflate(0.5), e -> e.getTags().contains(glitch.tag))) {
                display.discard();
            }
            if (glitch.torn && glitch.level.getBlockState(glitch.pos).isAir()) {
                glitch.level.setBlock(glitch.pos, glitch.original, Block.UPDATE_ALL);
            }
            ACTIVE_TAGS.remove(glitch.tag);
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
            if (random.nextFloat() < PAIR_CHANCE) {
                BlockPos other = canGlitch(level, pos.above()) ? pos.above() : pos.below();
                if (canGlitch(level, other)) {
                    int endAt = now + 20 * (40 + random.nextInt(40));
                    start(level, pos, true, endAt);
                    start(level, other, true, endAt);
                    level.playSound(null, pos, ModSounds.GLITCH, SoundSource.BLOCKS, 1.4F, 0.6F);
                    return;
                }
            }
            start(level, pos, false, now + 20 * (8 + random.nextInt(22)));
            level.playSound(null, pos, ModSounds.GLITCH, SoundSource.BLOCKS, 0.8F, 0.8F + random.nextFloat() * 0.4F);
            return;
        }
    }

    private static void start(ServerLevel level, BlockPos pos, boolean torn, int endAt) {
        String tag = TAG + "_" + nextId++;
        Glitch glitch = new Glitch(level, pos.immutable(), level.getBlockState(pos), torn, tag, endAt);
        ACTIVE.add(glitch);
        ACTIVE_TAGS.add(tag);
        // A hair bigger than the block, so it sits over every face, and fully bright so it shows up in the dark
        String command = String.format(Locale.ROOT,
                "summon minecraft:block_display %d %d %d {Tags:[\"%s\",\"%s\"],block_state:{Name:\"backrooms:glitch\"},"
                        + "brightness:{sky:15,block:15},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                        + "translation:[-0.005f,-0.005f,-0.005f],scale:[1.01f,1.01f,1.01f]}}",
                pos.getX(), pos.getY(), pos.getZ(), TAG, tag);
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(source, command);
        if (torn) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Ordinary solid full blocks only: nothing with contents, nothing unbreakable, nothing already glitching. */
    private static boolean canGlitch(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        for (Glitch glitch : ACTIVE) {
            if (glitch.level == level && glitch.pos.equals(pos)) {
                return false;
            }
        }
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
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
