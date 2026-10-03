package com.hackclient.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ItemUtil {
	private ItemUtil() {}

	public static boolean isSpear(ItemStack stack) {
		return stack.has(DataComponents.PIERCING_WEAPON);
	}

	public static boolean isWeapon(ItemStack stack) {
		return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.MACE) || stack.is(Items.TRIDENT) || isSpear(stack);
	}
}
