package com.hackclient.module.modules.render;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/** Hooked in GameRendererMixin. */
public class Zoom extends Module {
	private final NumberSetting factor = number("Factor", 4.0, 1.5, 10.0, 1);

	public Zoom() {
		super("Zoom", "Zooms in like a spyglass.", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	public float modifyFov(float fov) {
		return fov / factor.getFloat();
	}
}
