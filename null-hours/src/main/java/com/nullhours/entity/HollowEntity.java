package com.nullhours.entity;

import com.nullhours.haunt.SpawnUtil;
import com.nullhours.net.ScarePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * A tall black figure with white eyes. It stands far away and watches. Stare back and it
 * either fades away or comes running.
 */
public class HollowEntity extends HauntEntity {
	private float chaseChance;
	private boolean chasing;
	private int stareTicks;
	private int chaseTicks;

	public HollowEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 100.0)
				.add(Attributes.MOVEMENT_SPEED, 0.34)
				.add(Attributes.FOLLOW_RANGE, 96.0)
				.add(Attributes.SCALE, 1.35)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	/** Chance that it charges instead of fading when the player stares at it. */
	public void setChaseChance(float chance) {
		this.chaseChance = chance;
	}

	public void startChase(ServerLevel level) {
		chasing = true;
		playSound(level, SoundEvents.ENDERMAN_SCREAM, 3.0f, 0.5f);
		playSound(level, SoundEvents.WARDEN_ROAR, 1.5f, 1.4f);
	}

	@Override
	protected int lifetime() {
		return 20 * 90;
	}

	@Override
	protected void hauntTick(ServerLevel level, ServerPlayer victim) {
		lookAtVictim(victim);
		double distance = distanceTo(victim);
		if (!chasing) {
			getNavigation().stop();
			if (distance < 12) {
				vanish(level);
				return;
			}
			stareTicks = isSeenBy(victim, 0.985) ? stareTicks + 1 : Math.max(0, stareTicks - 1);
			if (stareTicks > 20) {
				if (getRandom().nextFloat() < chaseChance) startChase(level);
				else vanish(level);
			}
			return;
		}

		chaseTicks++;
		if (distance < 2.2) {
			jumpscare(level, victim, ScarePayload.FACE_HOLLOW);
			return;
		}
		if (chaseTicks > 20 * 20) {
			vanish(level);
			return;
		}
		getNavigation().moveTo(victim, 2.0);
		// If it cannot find a path it blinks closer instead of getting stuck
		if (chaseTicks % 40 == 0 && getNavigation().isDone() && distance > 6) {
			BlockPos spot = SpawnUtil.around(level, victim, victim.getYRot() + 180 + getRandom().nextInt(90) - 45, Math.min(distance - 3, 10), 3);
			if (spot != null) teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
		}
	}
}
