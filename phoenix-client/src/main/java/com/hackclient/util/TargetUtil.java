package com.hackclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TargetUtil {
	private static final Minecraft mc = Minecraft.getInstance();

	private TargetUtil() {}

	public static boolean isValidTarget(Entity entity, boolean players, boolean mobs, boolean animals) {
		if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive()) return false;
		if (entity instanceof Player player) return players && !player.isSpectator();
		if (entity instanceof Enemy) return mobs;
		if (entity instanceof Animal) return animals;
		return false;
	}

	/** Closest valid target within range, or null. */
	public static LivingEntity findClosest(double range, boolean players, boolean mobs, boolean animals) {
		LivingEntity best = null;
		double bestDist = range * range;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!isValidTarget(entity, players, mobs, animals)) continue;
			double dist = mc.player.distanceToSqr(entity);
			if (dist <= bestDist) {
				bestDist = dist;
				best = (LivingEntity) entity;
			}
		}
		return best;
	}

	/** {yaw, pitch} that makes the player face the middle of the entity. */
	public static float[] rotationsTo(Entity entity) {
		Vec3 eyes = mc.player.getEyePosition();
		Vec3 target = entity.position().add(0, entity.getBbHeight() / 2, 0);
		double dx = target.x - eyes.x;
		double dy = target.y - eyes.y;
		double dz = target.z - eyes.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);

		float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
		return new float[] {yaw, pitch};
	}

	/** Rotates the player to face the middle of the entity. */
	public static void lookAt(Entity entity) {
		float[] rotations = rotationsTo(entity);
		mc.player.setYRot(rotations[0]);
		mc.player.setXRot(rotations[1]);
	}

	/** Degrees between where the player is looking and the middle of the entity. */
	public static double angleTo(Entity entity) {
		Vec3 look = mc.player.getLookAngle();
		Vec3 toTarget = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(mc.player.getEyePosition()).normalize();
		return Math.toDegrees(Math.acos(Math.clamp(look.dot(toTarget), -1.0, 1.0)));
	}

	/** Distance from the player's eyes to the nearest point of the entity's hitbox (how vanilla measures reach). */
	public static double reachDistance(Entity entity) {
		return Math.sqrt(entity.getBoundingBox().distanceToSqr(mc.player.getEyePosition()));
	}
}
