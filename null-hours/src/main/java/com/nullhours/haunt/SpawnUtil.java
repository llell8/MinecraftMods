package com.nullhours.haunt;

import com.nullhours.entity.HauntEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SpawnUtil {
	private SpawnUtil() {
	}

	/**
	 * Finds standing room at the given distance from the player, in the direction of the
	 * given yaw (0 is south, like the player's own yaw). Returns null when there is none.
	 */
	public static BlockPos around(ServerLevel level, Player player, double yaw, double distance, int height) {
		double rad = Math.toRadians(yaw);
		int x = Mth.floor(player.getX() - Math.sin(rad) * distance);
		int z = Mth.floor(player.getZ() + Math.cos(rad) * distance);
		return ground(level, x, player.getBlockY(), z, height);
	}

	/** Searches down from a little above y for a solid block with enough air on top. */
	public static BlockPos ground(ServerLevel level, int x, int y, int z, int height) {
		for (int dy = 6; dy >= -10; dy--) {
			BlockPos pos = new BlockPos(x, y + dy, z);
			if (!solid(level, pos.below())) continue;
			boolean room = true;
			for (int h = 0; h < height && room; h++) room = empty(level, pos.above(h));
			if (room) return pos;
		}
		return null;
	}

	private static boolean empty(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
	}

	private static boolean solid(ServerLevel level, BlockPos pos) {
		return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	public static boolean isInView(Player player, BlockPos pos, double minDot) {
		Vec3 to = Vec3.atCenterOf(pos).subtract(player.getEyePosition());
		double length = to.length();
		return length < 1.0e-4 || player.getViewVector(1.0f).dot(to.scale(1.0 / length)) > minDot;
	}

	/** Yaw that looks from one point towards another. */
	public static float yawTowards(double fromX, double fromZ, double toX, double toZ) {
		return (float) Math.toDegrees(Math.atan2(toZ - fromZ, toX - fromX)) - 90.0f;
	}

	public static <T extends HauntEntity> T spawn(EntityType<T> type, ServerLevel level, BlockPos pos, Player victim) {
		T entity = type.create(level, EntitySpawnReason.EVENT);
		if (entity == null) return null;
		double x = pos.getX() + 0.5;
		double z = pos.getZ() + 0.5;
		float yaw = yawTowards(x, z, victim.getX(), victim.getZ());
		entity.snapTo(x, pos.getY(), z, yaw, 0.0f);
		entity.setYHeadRot(yaw);
		entity.setYBodyRot(yaw);
		entity.setVictim(victim);
		level.addFreshEntity(entity);
		return entity;
	}
}
