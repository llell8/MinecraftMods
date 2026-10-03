package com.hackclient.module.modules.grinding;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Eats food from your hotbar when you get hungry, then switches back. */
public class AutoEat extends Module {
	private final NumberSetting hunger = number("Eat at hunger", 14, 1, 19, 0);

	private boolean eating;
	private int previousSlot = -1;

	public AutoEat() {
		super("AutoEat", "Eats from your hotbar when hungry.", Category.GRINDING, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		if (eating) stopEating();
	}

	@Override
	public void onTick() {
		int food = mc.player.getFoodData().getFoodLevel();

		if (eating) {
			if (food >= 20 || mc.screen != null || !isGoodFood(mc.player.getMainHandItem())) stopEating();
			else mc.options.keyUse.setDown(true);
			return;
		}

		if (food > hunger.get() || mc.screen != null || mc.player.isUsingItem()) return;

		int slot = findFood();
		if (slot == -1) return;

		Inventory inventory = mc.player.getInventory();
		previousSlot = inventory.getSelectedSlot();
		inventory.setSelectedSlot(slot);
		mc.options.keyUse.setDown(true);
		eating = true;
	}

	private void stopEating() {
		mc.options.keyUse.setDown(false);
		if (mc.player != null && previousSlot != -1) mc.player.getInventory().setSelectedSlot(previousSlot);
		previousSlot = -1;
		eating = false;
	}

	private int findFood() {
		Inventory inventory = mc.player.getInventory();
		int best = -1;
		int bestNutrition = 0;
		for (int i = 0; i < 9; i++) {
			ItemStack stack = inventory.getItem(i);
			if (!isGoodFood(stack)) continue;
			int nutrition = stack.get(DataComponents.FOOD).nutrition();
			if (nutrition > bestNutrition) {
				bestNutrition = nutrition;
				best = i;
			}
		}
		return best;
	}

	private static boolean isGoodFood(ItemStack stack) {
		if (!stack.has(DataComponents.FOOD)) return false;
		// Skip foods with bad side effects
		return !stack.is(Items.ROTTEN_FLESH) && !stack.is(Items.SPIDER_EYE) && !stack.is(Items.POISONOUS_POTATO)
				&& !stack.is(Items.PUFFERFISH) && !stack.is(Items.CHORUS_FRUIT) && !stack.is(Items.SUSPICIOUS_STEW)
				&& !stack.is(Items.GOLDEN_APPLE) && !stack.is(Items.ENCHANTED_GOLDEN_APPLE);
	}
}
