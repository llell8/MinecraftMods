package com.hackclient.module.modules.movement;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Lets you jump again while in the air: each press of jump gives you a fresh jump. */
public class AirJump extends Module {
	private static final double JUMP_POWER = 0.42; // vanilla jump

	private final BoolSetting maintainLevel = bool("Maintain level", false);

	private boolean jumpWasDown;
	private double levelY;

	public AirJump() {
		super("AirJump", "Jump again in mid-air, as many times as you like.", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onEnable() {
		if (mc.player != null) levelY = mc.player.getY();
	}

	@Override
	public void onTick() {
		if (mc.player.getAbilities().flying || mc.player.isFallFlying() || mc.player.isInWater() || mc.player.isPassenger()) return;
		boolean jumpDown = mc.options.keyJump.isDown();
		Vec3 velocity = mc.player.getDeltaMovement();

		if (jumpDown && !jumpWasDown && !mc.player.onGround()) {
			mc.player.setDeltaMovement(velocity.x, JUMP_POWER, velocity.z);
			levelY = mc.player.getY();
		} else if (maintainLevel.get() && !jumpDown && !mc.player.onGround() && mc.player.getY() <= levelY && velocity.y < 0) {
			// Hover at the height of your last air jump instead of falling
			mc.player.setDeltaMovement(velocity.x, 0, velocity.z);
		}
		if (mc.player.onGround()) levelY = mc.player.getY();
		jumpWasDown = jumpDown;
	}
}
