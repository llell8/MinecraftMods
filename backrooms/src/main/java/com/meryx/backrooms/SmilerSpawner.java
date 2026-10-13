package com.meryx.backrooms;

import com.meryx.backrooms.entity.Bacteria;
import com.meryx.backrooms.entity.ModEntities;
import com.meryx.backrooms.entity.Smiler;
import com.meryx.backrooms.world.Maze;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;

/**
 * Puts Smilers in the dark places around each survival player who has wandered the Backrooms for a while, now and then
 * lets a Bacteria loose somewhere out of sight, and every so often screams in their ears when nothing is there at all.
 */
public final class SmilerSpawner {
    private static final int CHECK_INTERVAL = 40;
    /** Ticks in Level 0 before the first Smiler can show up. */
    private static final int GRACE_TICKS = 20 * 20;
    private static final double NEARBY = 48.0;
    private static final int MAX_NEARBY = 3;
    /** Ticks in Level 0 before a Bacteria can show up, and the chance per check after that. */
    private static final int BACTERIA_GRACE_TICKS = 20 * 60;
    private static final int BACTERIA_ONE_IN = 20;
    private static final double BACTERIA_RANGE = 64.0;
    /** A Bacteria's scream with no Bacteria: about once every three minutes, per check after the grace period. */
    private static final int PHANTOM_ONE_IN = 90;

    private static final Map<UUID, Integer> ENTERED_AT = new HashMap<>();

    private SmilerSpawner() {
    }

    static void entered(ServerPlayer player) {
        ENTERED_AT.put(player.getUUID(), player.level().getServer().getTickCount());
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        if (now % CHECK_INTERVAL != 0) {
            return;
        }
        for (ResourceKey<Level> key : Backrooms.LEVELS) {
            ServerLevel level = server.getLevel(key);
            if (level != null && level.getDifficulty() != Difficulty.PEACEFUL) {
                tick(level, now);
            }
        }
    }

    private static void tick(ServerLevel level, int now) {
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            int entered = ENTERED_AT.computeIfAbsent(player.getUUID(), id -> now);
            boolean bacteriaNearby = !level.getEntitiesOfClass(Bacteria.class, player.getBoundingBox().inflate(BACTERIA_RANGE)).isEmpty();
            if (now - entered >= GRACE_TICKS && !bacteriaNearby && random.nextInt(PHANTOM_ONE_IN) == 0) {
                // Nothing there. It screams anyway, right in your ears.
                ModSounds.playInEars(player, ModSounds.BACTERIA_SCREAM, 0.85F + random.nextFloat() * 0.3F);
            }
            if (now - entered >= BACTERIA_GRACE_TICKS && random.nextInt(BACTERIA_ONE_IN) == 0
                    && !bacteriaNearby) {
                spawnBacteria(level, player, random);
            }
            if (now - entered < GRACE_TICKS || random.nextInt(4) != 0) {
                continue;
            }
            if (level.getEntitiesOfClass(Smiler.class, player.getBoundingBox().inflate(NEARBY)).size() >= MAX_NEARBY) {
                continue;
            }
            // Only where the lights are dead: in a well-lit area you are safe
            BlockPos spot = Smiler.findDarkSpot(level, player.blockPosition(), 2, 6, random);
            if (spot == null) {
                continue;
            }
            Smiler smiler = (random.nextBoolean() ? ModEntities.SMILER : ModEntities.VHS_SMILER).create(level, EntitySpawnReason.EVENT);
            if (smiler != null) {
                smiler.setPos(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
                if (player.hasLineOfSight(smiler)) {
                    // It is already there when you look; it never appears in front of you
                    smiler.discard();
                    continue;
                }
                level.addFreshEntity(smiler);
            }
        }
    }

    /** Somewhere a few rooms away that the player cannot see. */
    private static void spawnBacteria(ServerLevel level, ServerPlayer player, RandomSource random) {
        int cellX = Math.floorDiv(player.getBlockX(), Maze.CELL);
        int cellZ = Math.floorDiv(player.getBlockZ(), Maze.CELL);
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = random.nextInt(9) - 4;
            int dz = random.nextInt(9) - 4;
            if (Math.max(Math.abs(dx), Math.abs(dz)) < 3) {
                continue;
            }
            BlockPos pos = new BlockPos((cellX + dx) * Maze.CELL + Maze.CELL / 2, Maze.STAND_Y, (cellZ + dz) * Maze.CELL + Maze.CELL / 2);
            if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above(2)).isAir()) {
                continue;
            }
            Bacteria bacteria = ModEntities.BACTERIA.create(level, EntitySpawnReason.EVENT);
            if (bacteria == null) {
                return;
            }
            bacteria.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (player.hasLineOfSight(bacteria)) {
                bacteria.discard();
                continue;
            }
            level.addFreshEntity(bacteria);
            return;
        }
    }
}
