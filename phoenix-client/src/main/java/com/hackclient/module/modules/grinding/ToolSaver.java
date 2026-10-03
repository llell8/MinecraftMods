package com.hackclient.module.modules.grinding;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Stops you from mining/attacking with a tool that's about to break. Hooked in MultiPlayerGameModeMixin. */
public class ToolSaver extends Module {
	private final NumberSetting minDurability = number("Min durability", 10, 1, 100, 0);

	public ToolSaver() {
		super("ToolSaver", "Stops you breaking your tools.", Category.GRINDING, GLFW.GLFW_KEY_UNKNOWN);
	}

	/** @return true if the action should be blocked */
	public boolean shouldBlock() {
		ItemStack stack = mc.player.getMainHandItem();
		if (!stack.isDamageableItem()) return false;
		int left = stack.getMaxDamage() - stack.getDamageValue();
		if (left > minDurability.get()) return false;

		mc.gui.setOverlayMessage(Component.literal("ToolSaver: " + left + " durability left!").withStyle(ChatFormatting.RED), false);
		return true;
	}
}
