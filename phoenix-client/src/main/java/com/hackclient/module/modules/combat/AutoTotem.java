package com.hackclient.module.modules.combat;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class AutoTotem extends Module {
	public AutoTotem() {
		super("AutoTotem", "Keeps a totem of undying in your offhand.", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
		if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) return;

		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < 36; i++) {
			if (!inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) continue;

			// Inventory index -> player container slot (hotbar is 36-44, main inventory is 9-35)
			int slot = i < 9 ? 36 + i : i;
			mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, Inventory.SLOT_OFFHAND, ClickType.SWAP, mc.player);
			return;
		}
	}
}
