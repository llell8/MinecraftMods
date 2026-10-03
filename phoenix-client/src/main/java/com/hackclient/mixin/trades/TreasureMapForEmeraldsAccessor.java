package com.hackclient.mixin.trades;

import net.minecraft.world.entity.npc.villager.VillagerTrades;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VillagerTrades.TreasureMapForEmeralds.class)
public interface TreasureMapForEmeraldsAccessor {
	@Accessor("emeraldCost")
	int getEmeraldCost();

	@Accessor("displayName")
	String getDisplayName();
}
