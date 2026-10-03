package com.hackclient.mixin.trades;

import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VillagerTrades.TippedArrowForItemsAndEmeralds.class)
public interface TippedArrowForItemsAndEmeraldsAccessor {
	@Accessor("toItem")
	ItemStack getToItem();

	@Accessor("toCount")
	int getToCount();

	@Accessor("emeraldCost")
	int getEmeraldCost();

	@Accessor("fromItem")
	Item getFromItem();

	@Accessor("fromCount")
	int getFromCount();
}
