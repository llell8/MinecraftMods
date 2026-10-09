package com.nullhours.entity;

import com.nullhours.NullHoursConfig;
import com.nullhours.net.ScarePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Shared behaviour of the haunting entities: each one is tied to the player it was sent
 * after, cannot be killed, and leaves on its own after a while.
 */
public abstract class HauntEntity extends Monster {
	private UUID victimId;

	protected HauntEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setSilent(true);
	}

	public void setVictim(Player player) {
		this.victimId = player.getUUID();
	}

	/** How long the entity stays before it leaves by itself, in ticks. */
	protected abstract int lifetime();

	/** Runs every server tick while the victim is nearby. */
	protected abstract void hauntTick(ServerLevel level, ServerPlayer victim);

	/** Called when the victim hits the entity. */
	protected void onHit(ServerLevel level, ServerPlayer victim) {
		vanish(level);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level) || isRemoved()) return;
		ServerPlayer victim = victim(level);
		if (victim == null || !victim.isAlive() || victim.isSpectator() || distanceToSqr(victim) > 128 * 128 || tickCount > lifetime()) {
			discard();
			return;
		}
		hauntTick(level, victim);
	}

	private ServerPlayer victim(ServerLevel level) {
		if (victimId == null) {
			Player nearest = level.getNearestPlayer(this, 64);
			if (nearest == null) return null;
			victimId = nearest.getUUID();
		}
		return level.getPlayerByUUID(victimId) instanceof ServerPlayer player ? player : null;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Only /kill and the void get rid of them; hitting one just makes it react
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, amount);
		if (source.getEntity() instanceof ServerPlayer player && !isRemoved()) onHit(level, player);
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	/** True when the entity is inside the player's view cone and nothing blocks the line of sight. */
	protected boolean isSeenBy(Player player, double minDot) {
		Vec3 view = player.getViewVector(1.0f);
		Vec3 toMe = new Vec3(getX() - player.getX(), getY(0.75) - player.getEyeY(), getZ() - player.getZ());
		double length = toMe.length();
		if (length < 1.0e-4) return true;
		return view.dot(toMe.scale(1.0 / length)) > minDot && player.hasLineOfSight(this);
	}

	protected void lookAtVictim(ServerPlayer victim) {
		getLookControl().setLookAt(victim, 180.0f, 180.0f);
	}

	protected void playSound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, volume, pitch);
	}

	/** Disappears in a puff of smoke. */
	public void vanish(ServerLevel level) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() / 2, getZ(), 25, 0.3, getBbHeight() / 3, 0.3, 0.01);
		playSound(level, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 0.4f);
		discard();
	}

	/** Fills the victim's screen with this entity's face, hurts them, and disappears. */
	protected void jumpscare(ServerLevel level, ServerPlayer victim, int face) {
		ServerPlayNetworking.send(victim, new ScarePayload(ScarePayload.JUMPSCARE, face, getX(), getEyeY(), getZ(), ""));
		float damage = NullHoursConfig.INSTANCE.jumpscareDamage;
		if (damage > 0 && !victim.isCreative()) {
			victim.hurtServer(level, level.damageSources().mobAttack(this), damage);
		}
		discard();
	}
}
