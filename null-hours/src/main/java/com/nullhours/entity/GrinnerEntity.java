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

/** Stands right behind you. Turn around and it is in your face. */
public class GrinnerEntity extends HauntEntity {
	private int repositions;

	public GrinnerEntity(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.0)
				.add(Attributes.SCALE, 1.1);
	}

	@Override
	protected int lifetime() {
		return 20 * 30;
	}

	@Override
	protected void hauntTick(ServerLevel level, ServerPlayer victim) {
		lookAtVictim(victim);
		if (tickCount > 5 && isSeenBy(victim, 0.7)) {
			jumpscare(level, victim, ScarePayload.FACE_GRINNER);
			return;
		}
		if (tickCount % 60 == 0) {
			playSound(level, SoundEvents.WARDEN_HEARTBEAT, 0.5f, 0.6f);
		}
		// Keeps up with the player so it is always just behind them
		if (tickCount % 10 == 0 && distanceTo(victim) > 4.5 && repositions < 8) {
			BlockPos spot = SpawnUtil.around(level, victim, victim.getYRot() + 180, 2.5, 3);
			if (spot != null && !SpawnUtil.isInView(victim, spot, 0.3)) {
				teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
				repositions++;
			}
		}
	}

	@Override
	protected void onHit(ServerLevel level, ServerPlayer victim) {
		jumpscare(level, victim, ScarePayload.FACE_GRINNER);
	}
}
