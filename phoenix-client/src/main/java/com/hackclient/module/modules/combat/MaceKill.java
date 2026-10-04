package com.hackclient.module.modules.combat;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import com.hackclient.util.ElytraUtil;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

/**
 * Fakes a big fall right before a mace hit so it lands as a huge smash attack.
 * Hooked in MultiPlayerGameModeMixin.
 *
 * Vanilla servers allow ~10 blocks of movement per move packet, stacking up to 5 packets per tick,
 * so we send a few "stand still" packets first to raise the limit, then jump up and back down.
 * Singleplayer has no limit, so any height works there.
 */
public class MaceKill extends Module {
	private static final int MAX_FILLER_PACKETS = 3; // leave room for the client's own move packet this tick

	private final NumberSetting height = number("Height", 20, 2, 100, 0);

	public MaceKill() {
		super("MaceKill", "Turns mace hits into massive smash attacks.", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	/** @return true if a fake fall was sent (so Criticals doesn't also run) */
	public boolean onAttack(Entity target) {
		if (!(target instanceof LivingEntity)) return false;
		if (!mc.player.getMainHandItem().is(Items.MACE) || mc.player.isPassenger()) return false;
		// Mid-glide smashes don't count, so stop gliding first
		ElytraUtil.cancelGlide();

		double fall = clearHeightAbove(height.get());
		if (fall < 2) return false;

		double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
		boolean collision = mc.player.horizontalCollision;

		// Each extra packet this tick adds another ~10 blocks (100 squared) to the server's movement limit
		int filler = Math.min(MAX_FILLER_PACKETS, (int) Math.ceil(fall * fall / 100.0) - 1);
		for (int i = 0; i < filler; i++) {
			mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, mc.player.onGround(), collision));
		}
		mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + fall, z, false, collision));
		mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, collision));
		return true;
	}

	/** Highest height up to {@code max} with nothing solid between you and it (the server simulates the move). */
	private double clearHeightAbove(double max) {
		AABB box = mc.player.getBoundingBox();
		for (double h = Math.floor(max); h >= 2; h--) {
			if (mc.level.noCollision(mc.player, box.expandTowards(0, h, 0))) return h;
		}
		return 0;
	}
}
