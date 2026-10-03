package com.hackclient.module.modules.movement;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Speed extends Module {
	private final NumberSetting speed = number("Speed", 0.35, 0.1, 2.0, 2);
	private final BoolSetting onlyOnGround = bool("Only on ground", false);

	public Speed() {
		super("Speed", "Makes you move faster.", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		if (mc.player.getAbilities().flying || mc.player.isFallFlying() || mc.player.isInWater()) return;
		if (onlyOnGround.get() && !mc.player.onGround()) return;

		float forward = mc.player.zza;
		float strafe = mc.player.xxa;
		if (forward == 0 && strafe == 0) return;

		// Same rotation Minecraft uses to turn input into movement
		double yaw = Math.toRadians(mc.player.getYRot());
		double x = strafe * Math.cos(yaw) - forward * Math.sin(yaw);
		double z = forward * Math.cos(yaw) + strafe * Math.sin(yaw);
		double length = Math.sqrt(x * x + z * z);

		Vec3 velocity = mc.player.getDeltaMovement();
		mc.player.setDeltaMovement(x / length * speed.get(), velocity.y, z / length * speed.get());
	}
}
