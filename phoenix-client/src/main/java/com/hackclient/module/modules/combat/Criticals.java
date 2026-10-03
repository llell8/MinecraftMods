package com.hackclient.module.modules.combat;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

/** Fakes a tiny jump before each hit so it counts as a critical. Hooked in MultiPlayerGameModeMixin. */
public class Criticals extends Module {
	public Criticals() {
		super("Criticals", "Makes every hit a critical hit.", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	public void onAttack(Entity target) {
		if (!(target instanceof LivingEntity) || !mc.player.onGround() || mc.player.isInWater() || mc.player.onClimbable()) return;

		double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
		boolean collision = mc.player.horizontalCollision;
		mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, collision));
		mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, collision));
	}
}
