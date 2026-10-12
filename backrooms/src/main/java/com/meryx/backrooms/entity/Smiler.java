package com.meryx.backrooms.entity;

import com.meryx.backrooms.ModSounds;
import com.meryx.backrooms.block.FluorescentLightBlock;
import com.meryx.backrooms.block.ModBlocks;
import com.meryx.backrooms.world.Maze;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A face in the dark. It waits where the lights are dead, watching whoever is nearest. Get too close and it lunges. If
 * light reaches it, or something hits it, it slips away to another dark spot.
 * <p>
 * The first time someone spots it, there is a chance every light around them dies at once. While the blackout lasts it
 * comes for them through the dark, and if it reaches them before the lights come back on, it bites.
 */
public class Smiler extends Monster {
    private static final double WATCH_RANGE = 48.0;
    private static final double LUNGE_RANGE = 3.0;
    private static final float DAMAGE = 14.0F;
    /** Block light at or below this is dark enough for a Smiler to wait in. */
    public static final int DARK = 4;
    /** Block light at or above this drives it out. */
    private static final int TOO_BRIGHT = 8;
    /** Chance that being spotted puts the lights out, how far around the player, and for how long. */
    private static final float BLACKOUT_CHANCE = 0.4F;
    private static final int BLACKOUT_RADIUS = 12;
    private static final int BLACKOUT_TICKS = 20 * 12;
    private static final double SPOT_RANGE = 32.0;

    private int attackCooldown;
    private boolean spotted;
    private int huntTicks;
    @Nullable
    private ServerPlayer prey;

    public Smiler(EntityType<? extends Smiler> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, WATCH_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE, DAMAGE);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel level && isAlive()) {
            serverTick(level);
        }
    }

    private void serverTick(ServerLevel level) {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (!spotted) {
            ServerPlayer spotter = spottedBy(level);
            if (spotter != null) {
                spotted = true;
                if (random.nextFloat() < BLACKOUT_CHANCE) {
                    blackout(level, spotter);
                }
            }
        }

        if (huntTicks > 0 && prey != null && prey.isAlive() && prey.level() == level) {
            huntTicks--;
            if (huntTicks % 10 == 0) {
                getNavigation().moveTo(prey, 1.0);
            }
            getLookControl().setLookAt(prey, 30.0F, 30.0F);
            if (attackCooldown == 0 && distanceTo(prey) < LUNGE_RANGE) {
                lunge(level, prey);
            }
            return;
        }
        if (huntTicks > 0 || prey != null) {
            huntTicks = 0;
            prey = null;
            getNavigation().stop();
        }
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(0.0, Math.min(motion.y, 0.0), 0.0);

        if (tickCount % 10 == 0 && level.getBrightness(LightLayer.BLOCK, BlockPos.containing(getEyePosition())) >= TOO_BRIGHT) {
            slipAway(level);
            return;
        }

        ServerPlayer watched = nearestPlayer(level, false);
        if (watched != null) {
            getLookControl().setLookAt(watched, 30.0F, 30.0F);
        }
        ServerPlayer prey = nearestPlayer(level, true);
        if (prey != null && attackCooldown == 0 && distanceTo(prey) < LUNGE_RANGE) {
            lunge(level, prey);
        }
    }

    /** A survival player looking right at it from close enough, if any. */
    @Nullable
    private ServerPlayer spottedBy(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive() || player.distanceTo(this) > SPOT_RANGE) {
                continue;
            }
            Vec3 toSmiler = getEyePosition().subtract(player.getEyePosition()).normalize();
            if (player.getViewVector(1.0F).normalize().dot(toSmiler) > 0.8 && player.hasLineOfSight(this)) {
                return player;
            }
        }
        return null;
    }

    /** Every working light around the player goes out, coming back on one by one when the blackout ends. */
    private void blackout(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-BLACKOUT_RADIUS, -2, -BLACKOUT_RADIUS),
                center.offset(BLACKOUT_RADIUS, 8, BLACKOUT_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.FLUORESCENT_LIGHT) && state.getValue(FluorescentLightBlock.LIT)) {
                FluorescentLightBlock.flicker(level, pos.immutable(), state, BLACKOUT_TICKS + random.nextInt(60));
            }
        }
        level.playSound(null, getX(), getY(), getZ(), ModSounds.SMILER_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.5F);
        prey = player;
        huntTicks = BLACKOUT_TICKS;
    }

    private void lunge(ServerLevel level, ServerPlayer target) {
        attackCooldown = 40;
        huntTicks = 0;
        prey = null;
        target.hurtServer(level, damageSources().mobAttack(this), DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120));
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.SMILER_SCREAM, SoundSource.HOSTILE, 1.5F, 1.0F);
        slipAway(level);
    }

    /** Players it watches, or with {@code prey} only the ones it will attack (not creative or spectating). */
    @Nullable
    private ServerPlayer nearestPlayer(ServerLevel level, boolean prey) {
        ServerPlayer nearest = null;
        double best = WATCH_RANGE * WATCH_RANGE;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive() || (prey && player.isCreative())) {
                continue;
            }
            double distance = distanceToSqr(player);
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    /** Vanishes and reappears in another dark spot nearby, or is gone for good if there is none. */
    private void slipAway(ServerLevel level) {
        level.playSound(null, getX(), getY(), getZ(), ModSounds.SMILER_BLINK, SoundSource.HOSTILE, 1.0F, 0.6F);
        BlockPos spot = findDarkSpot(level, blockPosition(), 2, 5, random);
        if (spot == null) {
            discard();
            return;
        }
        teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
    }

    /**
     * A dark cell centre in Level 0 between {@code minCells} and {@code maxCells} cells from {@code around}, where a
     * Smiler can stand, or null if a few tries find none.
     */
    @Nullable
    public static BlockPos findDarkSpot(ServerLevel level, BlockPos around, int minCells, int maxCells, RandomSource random) {
        int cellX = Math.floorDiv(around.getX(), Maze.CELL);
        int cellZ = Math.floorDiv(around.getZ(), Maze.CELL);
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = random.nextInt(maxCells * 2 + 1) - maxCells;
            int dz = random.nextInt(maxCells * 2 + 1) - maxCells;
            if (Math.max(Math.abs(dx), Math.abs(dz)) < minCells) {
                continue;
            }
            BlockPos pos = new BlockPos((cellX + dx) * Maze.CELL + Maze.CELL / 2, Maze.STAND_Y, (cellZ + dz) * Maze.CELL + Maze.CELL / 2);
            if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                continue;
            }
            if (level.getBrightness(LightLayer.BLOCK, pos.above(2)) <= DARK) {
                return pos;
            }
        }
        return null;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        slipAway(level);
        return false;
    }

    /** Stays in its spot rather than randomly despawning; it only goes once you are far away. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return distanceToClosestPlayer > 96.0 * 96.0;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ModSounds.SMILER_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    public float getSoundVolume() {
        return 2.0F;
    }
}
