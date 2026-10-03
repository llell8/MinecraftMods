package com.hackclient.mixin.trades;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(VillagerTrades.ItemsAndEmeraldsToItems.class)
public interface ItemsAndEmeraldsToItemsAccessor {
	@Accessor("fromItem")
	ItemCost getFromItem();

	@Accessor("emeraldCost")
	int getEmeraldCost();

	@Accessor("toItem")
	ItemStack getToItem();

	@Accessor("enchantmentProvider")
	Optional<ResourceKey<EnchantmentProvider>> getEnchantmentProvider();
}
