package com.hackclient.util;

import com.hackclient.mixin.trades.DyedArmorForEmeraldsAccessor;
import com.hackclient.mixin.trades.EmeraldForItemsAccessor;
import com.hackclient.mixin.trades.EmeraldsForVillagerTypeItemAccessor;
import com.hackclient.mixin.trades.EnchantBookForEmeraldsAccessor;
import com.hackclient.mixin.trades.EnchantedItemForEmeraldsAccessor;
import com.hackclient.mixin.trades.ItemsAndEmeraldsToItemsAccessor;
import com.hackclient.mixin.trades.ItemsForEmeraldsAccessor;
import com.hackclient.mixin.trades.TippedArrowForItemsAndEmeraldsAccessor;
import com.hackclient.mixin.trades.TreasureMapForEmeraldsAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the vanilla trade tables to show which trades a villager *could* get at a level.
 * The real trades are picked at random when the villager levels up, so this is a pool, not a prediction.
 */
public final class FutureTrades {
	private static final Minecraft mc = Minecraft.getInstance();

	/** How many trades a villager picks from the pool each level (see AbstractVillager#addOffersFromItemListings). */
	public static final int PICKS_PER_LEVEL = 2;

	/** @param label replaces the result's item name when not null */
	public record PossibleTrade(ItemStack costA, ItemStack costB, ItemStack result, String label) {}

	private FutureTrades() {}

	/** @return how many listings are in the pool for that level (0 if none) */
	public static int poolSize(Villager villager, int level) {
		Int2ObjectMap<VillagerTrades.ItemListing[]> table = table(villager);
		VillagerTrades.ItemListing[] listings = table != null ? table.get(level) : null;
		return listings != null ? listings.length : 0;
	}

	public static List<PossibleTrade> forLevel(Villager villager, int level) {
		List<PossibleTrade> result = new ArrayList<>();
		Int2ObjectMap<VillagerTrades.ItemListing[]> table = table(villager);
		if (table == null || table.get(level) == null) return result;

		for (VillagerTrades.ItemListing listing : table.get(level)) {
			PossibleTrade trade = describe(listing, villager);
			if (trade != null) result.add(trade);
		}
		return result;
	}

	private static Int2ObjectMap<VillagerTrades.ItemListing[]> table(Villager villager) {
		ResourceKey<VillagerProfession> profession = villager.getVillagerData().profession().unwrapKey().orElse(null);
		if (profession == null) return null;
		// Same choice the server makes in Villager#updateTrades
		if (mc.level.enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
			Int2ObjectMap<VillagerTrades.ItemListing[]> experimental = VillagerTrades.EXPERIMENTAL_TRADES.get(profession);
			if (experimental != null) return experimental;
		}
		return VillagerTrades.TRADES.get(profession);
	}

	private static PossibleTrade describe(VillagerTrades.ItemListing listing, Villager villager) {
		if (listing instanceof EmeraldForItemsAccessor trade) {
			return new PossibleTrade(trade.getCost().itemStack(), ItemStack.EMPTY, emeralds(trade.getEmeraldAmount()), null);
		}
		if (listing instanceof ItemsForEmeraldsAccessor trade) {
			ItemStack stack = trade.getItemStack().copy();
			String label = trade.getEnchantmentProvider().isPresent() ? "Enchanted " + name(stack) : null;
			return new PossibleTrade(emeralds(trade.getEmeraldCost()), ItemStack.EMPTY, stack, label);
		}
		if (listing instanceof ItemsAndEmeraldsToItemsAccessor trade) {
			ItemStack stack = trade.getToItem().copy();
			String label = trade.getEnchantmentProvider().isPresent() ? "Enchanted " + name(stack) : null;
			return new PossibleTrade(emeralds(trade.getEmeraldCost()), trade.getFromItem().itemStack(), stack, label);
		}
		if (listing instanceof EmeraldsForVillagerTypeItemAccessor trade) {
			ResourceKey<VillagerType> type = villager.getVillagerData().type().unwrapKey().orElse(null);
			Item item = type != null ? trade.getTrades().get(type) : null;
			if (item == null) return null;
			return new PossibleTrade(new ItemStack(item, trade.getCost()), ItemStack.EMPTY, emeralds(1), null);
		}
		if (listing instanceof EnchantBookForEmeraldsAccessor) {
			return new PossibleTrade(emeralds(5), new ItemStack(Items.BOOK), new ItemStack(Items.ENCHANTED_BOOK), "Random enchant (5-64 em)");
		}
		if (listing instanceof EnchantedItemForEmeraldsAccessor trade) {
			ItemStack stack = trade.getItemStack().copy();
			int min = Math.min(trade.getBaseEmeraldCost() + 5, 64);
			int max = Math.min(trade.getBaseEmeraldCost() + 19, 64);
			return new PossibleTrade(emeralds(min), ItemStack.EMPTY, stack, "Enchanted " + name(stack) + " (" + min + "-" + max + " em)");
		}
		if (listing instanceof DyedArmorForEmeraldsAccessor trade) {
			ItemStack stack = new ItemStack(trade.getItem());
			return new PossibleTrade(emeralds(trade.getValue()), ItemStack.EMPTY, stack, name(stack) + " (random dye)");
		}
		if (listing instanceof TippedArrowForItemsAndEmeraldsAccessor trade) {
			return new PossibleTrade(emeralds(trade.getEmeraldCost()), new ItemStack(trade.getFromItem(), trade.getFromCount()),
					new ItemStack(trade.getToItem().getItem(), trade.getToCount()), "Random tipped arrow");
		}
		if (listing instanceof TreasureMapForEmeraldsAccessor trade) {
			return new PossibleTrade(emeralds(trade.getEmeraldCost()), new ItemStack(Items.COMPASS), new ItemStack(Items.FILLED_MAP),
					Component.translatable(trade.getDisplayName()).getString());
		}
		if (listing instanceof VillagerTrades.SuspiciousStewForEmerald) {
			return new PossibleTrade(emeralds(1), ItemStack.EMPTY, new ItemStack(Items.SUSPICIOUS_STEW), null);
		}
		return null;
	}

	private static ItemStack emeralds(int count) {
		return new ItemStack(Items.EMERALD, count);
	}

	private static String name(ItemStack stack) {
		return stack.getHoverName().getString();
	}
}
