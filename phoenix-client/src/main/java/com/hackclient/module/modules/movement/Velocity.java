package com.hackclient.module.modules.movement;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Scales knockback you receive. Hooked in ClientPacketListenerMixin. */
public class Velocity extends Module {
	private final NumberSetting horizontal = number("Horizontal %", 0, 0, 100, 0);
	private final NumberSetting vertical = number("Vertical %", 0, 0, 100, 0);

	public Velocity() {
		super("Velocity", "Reduces knockback.", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	public Vec3 modify(Vec3 knockback) {
		double h = horizontal.get() / 100.0;
		double v = vertical.get() / 100.0;
		return new Vec3(knockback.x * h, knockback.y * v, knockback.z * h);
	}
}
