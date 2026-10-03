package com.hackclient.module.modules.render;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import org.lwjgl.glfw.GLFW;

/** Hooked in GameRendererMixin. */
public class NoHurtCam extends Module {
	public NoHurtCam() {
		super("NoHurtCam", "Removes the screen shake when you take damage.", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}
}
