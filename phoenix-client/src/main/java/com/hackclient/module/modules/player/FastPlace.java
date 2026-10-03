package com.hackclient.module.modules.player;

import com.hackclient.mixin.MinecraftAccessor;
import com.hackclient.module.Category;
import com.hackclient.module.Module;
import org.lwjgl.glfw.GLFW;

public class FastPlace extends Module {
	public FastPlace() {
		super("FastPlace", "Removes the delay between placing blocks.", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		((MinecraftAccessor) mc).setRightClickDelay(0);
	}
}
