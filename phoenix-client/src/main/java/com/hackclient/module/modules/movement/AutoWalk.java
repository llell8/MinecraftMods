package com.hackclient.module.modules.movement;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import org.lwjgl.glfw.GLFW;

public class AutoWalk extends Module {
	public AutoWalk() {
		super("AutoWalk", "Holds the forward key for you.", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		mc.options.keyUp.setDown(true);
	}

	@Override
	protected void onDisable() {
		mc.options.keyUp.setDown(false);
	}
}
