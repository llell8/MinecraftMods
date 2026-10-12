package com.meryx.backrooms.entity;

import com.meryx.backrooms.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Three blocks of black wire with a blob for a head and arms that reach the floor. It wanders Level 0, shaking, and
 * calls out in voices it has stolen ("Hello?", "Help me..."), and runs you down once it has seen you, screaming. Unlike a Smiler it can be killed, but it takes a lot of hitting.
 */
public class Bacteria extends Monster {
    public Bacteria(EntityType<? extends Bacteria> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // It screams the moment it spots you
        if (target != null && getTarget() == null) {
            scream();
        }
        super.setTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel && isAlive()) {
            // And now and then while it hunts, or just wanders
            if (random.nextInt(getTarget() != null ? 200 : 900) == 0) {
                scream();
            }
            // In between, it calls out in someone else's voice, trying to draw you closer
            if (random.nextInt(getTarget() != null ? 300 : 400) == 0) {
                level().playSound(null, getX(), getEyeY(), getZ(), ModSounds.BACTERIA_MIMIC, SoundSource.HOSTILE, 2.5F, 0.95F + random.nextFloat() * 0.1F);
            }
        }
    }

    private void scream() {
        float pitch = 0.9F + random.nextFloat() * 0.2F;
        level().playSound(null, getX(), getY(), getZ(), ModSounds.BACTERIA_SCREAM, SoundSource.HOSTILE, 4.0F, pitch);
        // Whoever it is after hears it right in their ears, at full volume, wherever it is
        if (getTarget() instanceof ServerPlayer player && distanceTo(player) < 32.0F) {
            ModSounds.playInEars(player, ModSounds.BACTERIA_SCREAM, pitch);
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return distanceToClosestPlayer > 96.0 * 96.0;
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ModSounds.BACTERIA_AMBIENT;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BACTERIA_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return ModSounds.BACTERIA_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F + random.nextFloat() * 0.2F;
    }
}
