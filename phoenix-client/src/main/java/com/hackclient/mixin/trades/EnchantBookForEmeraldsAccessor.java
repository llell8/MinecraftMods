package com.hackclient.mixin.trades;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VillagerTrades.EnchantBookForEmeralds.class)
public interface EnchantBookForEmeraldsAccessor {
	@Accessor("tradeableEnchantments")
	TagKey<Enchantment> getTradeableEnchantments();
}
