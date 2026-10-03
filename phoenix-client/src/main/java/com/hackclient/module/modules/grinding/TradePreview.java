package com.hackclient.module.modules.grinding;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.NumberSetting;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shows a villager's trades when you look at it.
 * With "Auto peek" it silently opens and closes the trade menu to read them.
 * Hooked in ClientPacketListenerMixin (packets) and TradePreviewHud (drawing).
 */
public class TradePreview extends Module {
	private final BoolSetting autoPeek = bool("Auto peek", true);
	private final NumberSetting refresh = number("Refresh (sec)", 30, 0, 300, 0);
	private final BoolSetting highlightBooks = bool("Glow max-level books", true);
	private final NumberSetting futureLevels = number("Future levels", 1, 0, 4, 0);

	public record CachedTrades(List<MerchantOffer> offers, int level, String profession, long time) {}

	private final Map<UUID, CachedTrades> cache = new HashMap<>();

	private Villager peekTarget;
	private int peekContainerId = -1;
	private int peekTimeout;
	private int peekCooldown;

	public TradePreview() {
		super("TradePreview", "Shows villager trades when you look at them.", Category.GRINDING, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		peekTarget = null;
		peekContainerId = -1;
	}

	@Override
	public void onTick() {
		if (peekCooldown > 0) peekCooldown--;
		if (peekTarget != null && --peekTimeout <= 0) {
			// Villager didn't open (busy, sleeping, baby...) - wait before trying again
			peekTarget = null;
			peekContainerId = -1;
			peekCooldown = 40;
		}

		if (!autoPeek.get() || peekTarget != null || peekCooldown > 0 || mc.screen != null) return;
		if (!(mc.crosshairPickEntity instanceof Villager villager) || !canTrade(villager)) return;
		if (mc.player.isShiftKeyDown()) return;

		// Don't use name tags / leads on the villager by accident
		ItemStack held = mc.player.getMainHandItem();
		if (held.is(Items.NAME_TAG) || held.is(Items.LEAD) || held.is(Items.VILLAGER_SPAWN_EGG)) return;

		CachedTrades cached = cache.get(villager.getUUID());
		if (cached != null && cached.profession().equals(professionId(villager))) {
			boolean fresh = refresh.get() == 0 || System.currentTimeMillis() - cached.time() < refresh.get() * 1000;
			if (fresh) return;
		}

		peekTarget = villager;
		peekTimeout = 20;
		mc.gameMode.interact(mc.player, villager, InteractionHand.MAIN_HAND);
	}

	/** @return true to stop the trade screen from opening (we're only peeking) */
	public boolean onOpenScreen(ClientboundOpenScreenPacket packet) {
		if (peekTarget == null || packet.getType() != MenuType.MERCHANT) return false;
		peekContainerId = packet.getContainerId();
		mc.getConnection().send(new ServerboundContainerClosePacket(peekContainerId));
		return true;
	}

	/** @return true if the packet belonged to a peek and should not reach the (unopened) menu */
	public boolean onMerchantOffers(ClientboundMerchantOffersPacket packet) {
		boolean peeked = peekTarget != null && packet.getContainerId() == peekContainerId;
		Entity source = peeked ? peekTarget : mc.crosshairPickEntity;

		if (source instanceof Villager villager) {
			List<MerchantOffer> offers = new ArrayList<>();
			for (MerchantOffer offer : packet.getOffers()) offers.add(offer.copy());
			cache.put(villager.getUUID(), new CachedTrades(offers, packet.getVillagerLevel(), professionId(villager), System.currentTimeMillis()));
		}

		if (peeked) {
			peekTarget = null;
			peekContainerId = -1;
		}
		return peeked;
	}

	public CachedTrades getTrades(Entity entity) {
		if (!(entity instanceof Villager villager)) return null;
		CachedTrades cached = cache.get(villager.getUUID());
		if (cached == null || !cached.profession().equals(professionId(villager))) return null;
		return cached;
	}

	/** How many levels above the current one to show possible trades for. */
	public int getFutureLevels() {
		return futureLevels.get().intValue();
	}

	public boolean shouldGlow(Entity entity) {
		if (!highlightBooks.get()) return false;
		CachedTrades trades = getTrades(entity);
		if (trades == null) return false;
		for (MerchantOffer offer : trades.offers()) {
			if (hasMaxLevelBook(offer.getResult())) return true;
		}
		return false;
	}

	public static boolean hasMaxLevelBook(ItemStack stack) {
		ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
		if (enchantments == null) return false;
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
			if (entry.getIntValue() >= entry.getKey().value().getMaxLevel()) return true;
		}
		return false;
	}

	private static boolean canTrade(Villager villager) {
		if (villager.isBaby()) return false;
		Holder<VillagerProfession> profession = villager.getVillagerData().profession();
		return !profession.is(VillagerProfession.NONE) && !profession.is(VillagerProfession.NITWIT);
	}

	public static String professionId(Villager villager) {
		return villager.getVillagerData().profession().unwrapKey().map(key -> key.identifier().getPath()).orElse("unknown");
	}
}
