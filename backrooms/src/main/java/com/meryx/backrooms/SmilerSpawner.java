package com.meryx.backrooms;

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
import net.minecraft.world.entity.EntitySpawnReason;

/** Sends a Smiler after each survival player who has wandered Level 0 for a while, somewhere out of sight. */
public final class SmilerSpawner {
    private static final int CHECK_INTERVAL = 40;
    /** Ticks in Level 0 before the first Smiler can show up. */
    private static final int GRACE_TICKS = 20 * 45;
    private static final double HUNT_RANGE = 80.0;

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
        ServerLevel level = server.getLevel(Backrooms.LEVEL_0);
        if (level == null || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            int entered = ENTERED_AT.computeIfAbsent(player.getUUID(), id -> now);
            if (now - entered < GRACE_TICKS || random.nextInt(12) != 0) {
                continue;
            }
            if (!level.getEntitiesOfClass(Smiler.class, player.getBoundingBox().inflate(HUNT_RANGE)).isEmpty()) {
                continue;
            }
            spawnNear(level, player, random);
        }
    }

    private static void spawnNear(ServerLevel level, ServerPlayer player, RandomSource random) {
        int playerCellX = Math.floorDiv(player.getBlockX(), Maze.CELL);
        int playerCellZ = Math.floorDiv(player.getBlockZ(), Maze.CELL);
        for (int attempt = 0; attempt < 10; attempt++) {
            int cellX = playerCellX + (random.nextBoolean() ? 1 : -1) * (3 + random.nextInt(3));
            int cellZ = playerCellZ + random.nextInt(11) - 5;
            if (random.nextBoolean()) {
                int swap = cellX - playerCellX;
                cellX = playerCellX + (cellZ - playerCellZ);
                cellZ = playerCellZ + swap;
            }
            BlockPos pos = new BlockPos(cellX * Maze.CELL + Maze.CELL / 2, Maze.STAND_Y, cellZ * Maze.CELL + Maze.CELL / 2);
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                continue;
            }
            Smiler smiler = ModEntities.SMILER.create(level, EntitySpawnReason.EVENT);
            if (smiler == null) {
                return;
            }
            smiler.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (player.hasLineOfSight(smiler)) {
                // Never appear in plain sight
                smiler.discard();
                continue;
            }
            level.addFreshEntity(smiler);
            return;
        }
    }
}
