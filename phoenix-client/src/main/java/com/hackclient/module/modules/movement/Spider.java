package com.hackclient.module.modules.movement;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Spider extends Module {
	private final NumberSetting speed = number("Speed", 0.2, 0.05, 1.0, 2);

	public Spider() {
		super("Spider", "Climb up walls like a spider.", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		if (!mc.player.horizontalCollision) return;
		Vec3 velocity = mc.player.getDeltaMovement();
		mc.player.setDeltaMovement(velocity.x, speed.get(), velocity.z);
	}
}
