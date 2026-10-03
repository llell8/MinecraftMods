package com.hackclient.util;

import com.hackclient.setting.HasIcon;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Predicate;

/** Broad categories of held items, for settings like "only run while holding X". */
public enum ItemType implements HasIcon {
	SWORD("Sword", Items.DIAMOND_SWORD, stack -> stack.is(ItemTags.SWORDS)),
	AXE("Axe", Items.DIAMOND_AXE, stack -> stack.is(ItemTags.AXES)),
	MACE("Mace", Items.MACE, stack -> stack.is(Items.MACE)),
	TRIDENT("Trident", Items.TRIDENT, stack -> stack.is(Items.TRIDENT)),
	SPEAR("Spear", Items.DIAMOND_SPEAR, ItemUtil::isSpear),
	PICKAXE("Pickaxe", Items.DIAMOND_PICKAXE, stack -> stack.is(ItemTags.PICKAXES)),
	SHOVEL("Shovel", Items.DIAMOND_SHOVEL, stack -> stack.is(ItemTags.SHOVELS)),
	HOE("Hoe", Items.DIAMOND_HOE, stack -> stack.is(ItemTags.HOES)),
	HAND("Empty hand", Items.PLAYER_HEAD, ItemStack::isEmpty),
	OTHER("Anything else", Items.CHEST, stack -> true);

	private final String display;
	private final Item iconItem;
	private final Predicate<ItemStack> matcher;

	ItemType(String display, Item iconItem, Predicate<ItemStack> matcher) {
		this.display = display;
		this.iconItem = iconItem;
		this.matcher = matcher;
	}

	/** The first (most specific) type that matches the stack. Always returns something, OTHER as a fallback. */
	public static ItemType of(ItemStack stack) {
		for (ItemType type : values()) {
			if (type.matcher.test(stack)) return type;
		}
		return OTHER;
	}

	@Override
	public ItemStack icon() {
		return new ItemStack(iconItem);
	}

	@Override
	public String toString() {
		return display;
	}
}
