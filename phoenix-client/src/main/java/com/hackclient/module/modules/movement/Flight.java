package com.hackclient.module.modules.movement;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/** Creative-style flight. Works in singleplayer; servers with anti-cheat will kick you. */
public class Flight extends Module {
	private final NumberSetting speed = number("Speed", 0.1, 0.01, 1.0, 2);

	public Flight() {
		super("Flight", "Lets you fly like in creative.", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		mc.player.getAbilities().mayfly = true;
		mc.player.getAbilities().flying = true;
		mc.player.getAbilities().setFlyingSpeed(speed.getFloat());
	}

	@Override
	protected void onDisable() {
		if (mc.player == null) return;
		boolean creative = mc.player.isCreative() || mc.player.isSpectator();
		mc.player.getAbilities().mayfly = creative;
		mc.player.getAbilities().flying = creative && mc.player.getAbilities().flying;
		mc.player.getAbilities().setFlyingSpeed(0.05f);
	}
}
