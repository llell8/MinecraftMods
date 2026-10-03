package com.hackclient.module.modules.player;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import net.minecraft.client.gui.screens.DeathScreen;
import org.lwjgl.glfw.GLFW;

public class AutoRespawn extends Module {
	public AutoRespawn() {
		super("AutoRespawn", "Respawns instantly when you die.", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		if (mc.screen instanceof DeathScreen) {
			mc.player.respawn();
			mc.setScreen(null);
		}
	}
}
