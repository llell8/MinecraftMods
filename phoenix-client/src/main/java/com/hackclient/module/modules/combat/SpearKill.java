package com.hackclient.module.modules.combat;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Makes spear charge attacks (hold right-click with a spear) hit extremely hard.
 *
 * Charge damage = base damage + (your speed towards the target × the spear's multiplier), and the server
 * takes "your speed" from how far your last position packet moved you. So right before the hit, we tell the
 * server we stepped back a few blocks and then snapped forward to where we really are: one tick of
 * huge forward speed, and we end exactly where we started.
 *
 * Vanilla servers allow ~10 blocks per move packet (more with a few extra packets in the same tick),
 * the same limit MaceKill works with. Singleplayer has no limit.
 */
public class SpearKill extends Module {
	private static final int MAX_FILLER_PACKETS = 2; // leave room for the client's own move packet this tick

	private final NumberSetting distance = number("Distance", 10, 2, 20, 1);
	private final NumberSetting cooldown = number("Cooldown (ticks)", 10, 1, 40, 0);

	private int cooldownTicks;

	public SpearKill() {
		super("SpearKill", "Hold right-click with a spear: charge hits become massive.", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		if (cooldownTicks > 0) {
			cooldownTicks--;
			return;
		}

		// Only while charging a spear, inside the window where the charge can deal damage
		if (!mc.player.isUsingItem() || mc.player.isPassenger() || mc.player.isFallFlying()) return;
		ItemStack spear = mc.player.getUseItem();
		KineticWeapon kinetic = spear.get(DataComponents.KINETIC_WEAPON);
		if (kinetic == null || mc.player.getTicksUsingItem() <= kinetic.delayTicks()) return;
		int chargeTicks = mc.player.getTicksUsingItem() - kinetic.delayTicks();
		if (kinetic.damageConditions().isPresent() && chargeTicks > kinetic.damageConditions().get().maxDurationTicks()) return;

		// Someone has to be in the spear's reach in front of us
		AttackRange range = spear.getOrDefault(DataComponents.ATTACK_RANGE, AttackRange.defaultFor(mc.player));
		if (!(range.getClosesetHit(mc.player, 1.0f, this::isTarget) instanceof EntityHitResult)) return;

		// Step back along where we're looking (flat, so no fall damage), then return
		Vec3 look = mc.player.getLookAngle();
		Vec3 direction = new Vec3(look.x, 0, look.z);
		if (direction.lengthSqr() < 1.0E-4) return; // looking straight up/down
		direction = direction.normalize();

		double back = clearDistanceBehind(direction, distance.get());
		if (back < 2) return;

		double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
		boolean onGround = mc.player.onGround();
		boolean collision = mc.player.horizontalCollision;

		// Each extra packet this tick adds another ~10 blocks (100 squared) to the server's movement limit
		int filler = Math.clamp((int) Math.ceil(back * back / 100.0) - 1, 0, MAX_FILLER_PACKETS);
		for (int i = 0; i < filler; i++) {
			mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, collision));
		}
		mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x - direction.x * back, y, z - direction.z * back, onGround, collision));
		// The last movement the server sees this tick is this one: `back` blocks forward in a single tick
		mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, collision));

		cooldownTicks = cooldown.get().intValue();
	}

	private boolean isTarget(Entity entity) {
		return entity instanceof LivingEntity && entity != mc.player && PiercingWeapon.canHitEntity(mc.player, entity);
	}

	/** How far we can step backwards (up to max) without walking into anything; the server simulates the move. */
	private double clearDistanceBehind(Vec3 direction, double max) {
		AABB box = mc.player.getBoundingBox();
		for (double d = max; d >= 2; d -= 0.5) {
			if (mc.level.noCollision(mc.player, box.expandTowards(-direction.x * d, 0, -direction.z * d))) return d;
		}
		return 0;
	}
}
