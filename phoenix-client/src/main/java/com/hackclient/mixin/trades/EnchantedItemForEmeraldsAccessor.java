package com.hackclient.mixin.trades;

import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VillagerTrades.EnchantedItemForEmeralds.class)
public interface EnchantedItemForEmeraldsAccessor {
	@Accessor("itemStack")
	ItemStack getItemStack();

	@Accessor("baseEmeraldCost")
	int getBaseEmeraldCost();
}
