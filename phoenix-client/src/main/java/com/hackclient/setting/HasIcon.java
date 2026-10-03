package com.hackclient.setting;

import net.minecraft.world.item.ItemStack;

/** Implemented by enum options that should show an item picture in the GUI. */
public interface HasIcon {
	ItemStack icon();
}
