package com.hackclient.mixin.trades;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(VillagerTrades.ItemsForEmeralds.class)
public interface ItemsForEmeraldsAccessor {
	@Accessor("itemStack")
	ItemStack getItemStack();

	@Accessor("emeraldCost")
	int getEmeraldCost();

	@Accessor("enchantmentProvider")
	Optional<ResourceKey<EnchantmentProvider>> getEnchantmentProvider();
}
