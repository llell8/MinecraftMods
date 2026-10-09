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
 * Follows a few blocks behind you without a sound. When you turn around it shrieks, stands
 * still for a moment so you can see it, then sprints at you. Run.
 */
public class GrinnerEntity extends HauntEntity {
	private static final int FREEZE_TICKS = 12;
	private static final int CHASE_TICKS = 20 * 8;

	private int repositions;
	private int spottedAt = -1;

	public GrinnerEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.SCALE, 1.1);
	}

	@Override
	protected int lifetime() {
		return 20 * 45;
	}

	@Override
	protected void hauntTick(ServerLevel level, ServerPlayer victim) {
		lookAtVictim(victim);
		double distance = distanceTo(victim);

		if (spottedAt < 0) {
			getNavigation().stop();
			if (tickCount > 5 && isSeenBy(victim, 0.7)) {
				spotted(level);
				return;
			}
			if (tickCount % 60 == 0) playSound(level, SoundEvents.WARDEN_HEARTBEAT, 0.5f, 0.6f);
			// Keeps up with the player so it is always a few steps behind them
			if (tickCount % 10 == 0 && distance > 9 && repositions < 8) {
				BlockPos spot = SpawnUtil.around(level, victim, victim.getYRot() + 180, 6, 3);
				if (spot != null && !SpawnUtil.isInView(victim, spot, 0.3)) {
					teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
					repositions++;
				}
			}
			return;
		}

		int sinceSpotted = tickCount - spottedAt;
		if (sinceSpotted < FREEZE_TICKS) {
			getNavigation().stop();
			return;
		}
		if (distance < 1.6) {
			jumpscare(level, victim, ScarePayload.FACE_GRINNER);
			return;
		}
		if (sinceSpotted > FREEZE_TICKS + CHASE_TICKS || distance > 28) {
			vanish(level);
			return;
		}
		getNavigation().moveTo(victim, 1.7);
	}

	private void spotted(ServerLevel level) {
		spottedAt = tickCount;
		playSound(level, SoundEvents.GHAST_SCREAM, 2.0f, 0.4f);
		playSound(level, SoundEvents.ENDERMAN_SCREAM, 2.0f, 0.6f);
	}

	@Override
	protected void onHit(ServerLevel level, ServerPlayer victim) {
		if (spottedAt < 0) spotted(level);
	}
}
