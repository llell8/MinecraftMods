package com.hackclient.mixin.trades;

import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VillagerTrades.EmeraldForItems.class)
public interface EmeraldForItemsAccessor {
	@Accessor("itemStack")
	ItemCost getCost();

	@Accessor("emeraldAmount")
	int getEmeraldAmount();
}
