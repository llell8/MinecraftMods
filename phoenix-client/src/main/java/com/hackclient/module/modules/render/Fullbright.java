package com.hackclient.module.modules.render;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

/** Applies a client-side night vision effect. */
public class Fullbright extends Module {
	public Fullbright() {
		super("Fullbright", "Lets you see in the dark.", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 420, 0, false, false));
	}

	@Override
	protected void onDisable() {
		if (mc.player != null) mc.player.removeEffect(MobEffects.NIGHT_VISION);
	}
}
