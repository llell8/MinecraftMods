package com.meryx.backrooms;

import com.meryx.backrooms.entity.ModEntities;
import com.meryx.backrooms.entity.Smiler;
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

/** Puts Smilers in the dark places around each survival player who has wandered Level 0 for a while. */
public final class SmilerSpawner {
    private static final int CHECK_INTERVAL = 40;
    /** Ticks in Level 0 before the first Smiler can show up. */
    private static final int GRACE_TICKS = 20 * 20;
    private static final double NEARBY = 48.0;
    private static final int MAX_NEARBY = 3;

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
}
