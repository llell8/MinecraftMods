package com.meryx.backrooms.entity;

import com.meryx.backrooms.ModSounds;
import com.meryx.backrooms.block.FluorescentLightBlock;
import com.meryx.backrooms.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
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
 * A tall, black, grinning thing. It only moves while nobody can see it: look straight at it under a working light and
 * it freezes; look away, or let the lights die, and it comes for you. Hitting it only makes it vanish and come back.
 */
public class Smiler extends Monster {
    private static final double VIEW_RANGE = 64.0;
    private static final float DAMAGE = 14.0F;

    private int attackCooldown;
    private int repathCooldown;
    private float frozenYaw;
    private boolean frozen;

    public Smiler(EntityType<? extends Smiler> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, VIEW_RANGE)
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
        if (isObserved(level)) {
            if (!frozen) {
                frozen = true;
                frozenYaw = getYRot();
            }
            getNavigation().stop();
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(0.0, Math.min(motion.y, 0.0), 0.0);
            setYRot(frozenYaw);
            setYHeadRot(frozenYaw);
            yBodyRot = frozenYaw;
            return;
        }
        frozen = false;

        ServerPlayer target = nearestTarget(level);
        if (target == null) {
            return;
        }
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--repathCooldown <= 0) {
            repathCooldown = 10;
            getNavigation().moveTo(target, 1.0);
        }
        if (random.nextInt(25) == 0) {
            killNearbyLights(level);
        }
        if (attackCooldown == 0 && distanceToSqr(target) < 3.5) {
            attack(level, target);
        }
    }

    private void attack(ServerLevel level, ServerPlayer target) {
        attackCooldown = 40;
        target.hurtServer(level, damageSources().mobAttack(this), DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120));
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.SMILER_SCREAM, SoundSource.HOSTILE, 1.5F, 1.0F);
        blinkAway(level);
    }

    /** Whether any player is looking at it and can actually see it. */
    private boolean isObserved(ServerLevel level) {
        BlockPos eyes = BlockPos.containing(getEyePosition());
        boolean lit = level.getBrightness(LightLayer.BLOCK, eyes) >= 4;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            double distance = player.distanceTo(this);
            if (distance > VIEW_RANGE || (!lit && distance > 3.0)) {
                continue;
            }
            Vec3 toSmiler = getEyePosition().subtract(player.getEyePosition()).normalize();
            if (player.getViewVector(1.0F).normalize().dot(toSmiler) > 0.55 && player.hasLineOfSight(this)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private ServerPlayer nearestTarget(ServerLevel level) {
        ServerPlayer nearest = null;
        double best = VIEW_RANGE * VIEW_RANGE;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
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

    /** The lights around it gutter out as it passes, and come back on a few seconds later. */
    private void killNearbyLights(ServerLevel level) {
        BlockPos center = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, 2, -5), center.offset(5, 6, 5))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.FLUORESCENT_LIGHT) && state.getValue(FluorescentLightBlock.LIT)) {
                FluorescentLightBlock.flicker(level, pos.immutable(), state, 80 + random.nextInt(100));
            }
        }
    }

    /** Vanishes and reappears somewhere out of the way. */
    private void blinkAway(ServerLevel level) {
        level.playSound(null, getX(), getY(), getZ(), ModSounds.SMILER_BLINK, SoundSource.HOSTILE, 1.0F, 0.6F);
        for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 20.0 + random.nextDouble() * 20.0;
            if (randomTeleport(getX() + Math.cos(angle) * distance, getY(), getZ() + Math.sin(angle) * distance, false)) {
                break;
            }
        }
        getNavigation().stop();
        repathCooldown = 40;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        blinkAway(level);
        return false;
    }

    /** Stays on your trail rather than randomly despawning; it only goes once you are far away. */
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
        // It makes no sound when it moves
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
