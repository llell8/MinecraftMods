package com.hackclient.module.modules.movement;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.lwjgl.glfw.GLFW;

public class Step extends Module {
	private static final double DEFAULT_STEP = 0.6;

	private final NumberSetting height = number("Height", 1.0, 0.6, 3.0, 1);

	public Step() {
		super("Step", "Walk up full blocks without jumping.", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		setStepHeight(height.get());
	}

	@Override
	protected void onDisable() {
		setStepHeight(DEFAULT_STEP);
	}

	private void setStepHeight(double value) {
		if (mc.player == null) return;
		AttributeInstance attribute = mc.player.getAttribute(Attributes.STEP_HEIGHT);
		if (attribute != null) attribute.setBaseValue(value);
	}
}
