package com.farlands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * A still, silent figure far away that faces you. It never moves. Stare at it
 * or walk toward it and it is simply gone.
 */
public final class Watcher {
    public static final String TAG = "farlands_watcher";

    private static final int LIFETIME = 20 * 40;
    private static final double VANISH_DISTANCE = 14;

    private static final Map<UUID, Sighting> ACTIVE = new HashMap<>();

    private static final class Sighting {
        final ServerPlayer player;
        final Mob figure;
        int age;
        int stared;

        Sighting(ServerPlayer player, Mob figure) {
            this.player = player;
            this.figure = figure;
        }
    }

    private Watcher() {}

    /** Places the figure somewhere behind the player, at the edge of what they can see. */
    public static boolean spawn(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return false;
        ServerLevel level = (ServerLevel) player.level();
        if (!level.canSeeSky(player.blockPosition())) return false;

        Vec3 behind = HorrorEvents.horizontalLook(player).scale(-1);
        double baseAngle = Math.atan2(behind.z, behind.x);
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = baseAngle + (player.getRandom().nextDouble() - 0.5) * Math.PI * 0.8;
            double dist = 28 + player.getRandom().nextDouble() * 16;
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            BlockPos column = new BlockPos(x, player.getBlockY(), z);
            if (!level.isLoaded(column)) continue;

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - player.getY()) > 20) continue;
            BlockPos feet = new BlockPos(x, y, z);
            if (!level.getBlockState(feet.below()).isSolid() || !level.getFluidState(feet).isEmpty()) continue;

            Mob figure = EntityType.HUSK.create(level, EntitySpawnReason.EVENT);
            if (figure == null) return false;
            figure.setPos(x + 0.5, y, z + 0.5);
            figure.setNoAi(true);
            figure.setSilent(true);
            figure.setInvulnerable(true);
            figure.setCanPickUpLoot(false);
            figure.setPersistenceRequired();
            figure.addTag(TAG);
            face(figure, player);
            // Track it first: the entity-load cleanup discards untracked figures
            ACTIVE.put(player.getUUID(), new Sighting(player, figure));
            level.addFreshEntity(figure);
            return true;
        }
        return false;
    }

    public static void tick(MinecraftServer server) {
        for (Iterator<Sighting> it = ACTIVE.values().iterator(); it.hasNext(); ) {
            Sighting s = it.next();
            ServerPlayer player = s.player;
            Mob figure = s.figure;

            if (figure.isRemoved() || player.isRemoved() || player.level() != figure.level()) {
                figure.discard();
                it.remove();
                continue;
            }

            face(figure, player);
            s.age++;

            double distance = player.distanceTo(figure);
            if (distance < VANISH_DISTANCE) {
                // You got too close. It was right there.
                scare(player, figure);
                figure.discard();
                it.remove();
                continue;
            }

            if (isLookingAt(player, figure)) {
                if (++s.stared > 15) {
                    puff(player, figure);
                    figure.discard();
                    it.remove();
                    continue;
                }
            }

            if (s.age > LIFETIME) {
                figure.discard();
                it.remove();
            }
        }
    }

    public static void vanish(ServerPlayer player, boolean withEffect) {
        Sighting s = ACTIVE.remove(player.getUUID());
        if (s == null) return;
        if (withEffect) puff(player, s.figure);
        s.figure.discard();
    }

    /** Cleans up a figure left behind by a crash; live ones are always tracked in ACTIVE. */
    public static void onEntityLoad(Entity entity) {
        if (!entity.getTags().contains(TAG)) return;
        for (Sighting s : ACTIVE.values()) if (s.figure == entity) return;
        entity.discard();
    }

    public static void vanishAll() {
        for (Sighting s : ACTIVE.values()) s.figure.discard();
        ACTIVE.clear();
    }

    private static boolean isLookingAt(ServerPlayer player, Mob figure) {
        Vec3 toFigure = figure.getEyePosition().subtract(player.getEyePosition()).normalize();
        return player.getLookAngle().dot(toFigure) > 0.985 && player.hasLineOfSight(figure);
    }

    private static void face(Mob figure, ServerPlayer player) {
        double dx = player.getX() - figure.getX();
        double dz = player.getZ() - figure.getZ();
        double dy = player.getEyeY() - figure.getEyeY();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
        figure.setYRot(yaw);
        figure.setYHeadRot(yaw);
        figure.setYBodyRot(yaw);
        figure.setXRot(pitch);
    }

    private static void puff(ServerPlayer player, Mob figure) {
        ServerLevel level = (ServerLevel) figure.level();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, figure.getX(), figure.getY() + 1, figure.getZ(),
                20, 0.3, 0.8, 0.3, 0.01);
        HorrorEvents.sound(player, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE,
                figure.position(), 0.4F, 0.3F);
    }

    private static void scare(ServerPlayer player, Mob figure) {
        puff(player, figure);
        HorrorEvents.sound(player, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, player.position(), 1.0F, 0.7F);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 4, 0, false, false));
    }
}
