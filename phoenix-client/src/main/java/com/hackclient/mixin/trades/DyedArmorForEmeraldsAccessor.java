package com.hackclient.mixin.trades;

import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VillagerTrades.DyedArmorForEmeralds.class)
public interface DyedArmorForEmeraldsAccessor {
	@Accessor("item")
	Item getItem();

	@Accessor("value")
	int getValue();
}
