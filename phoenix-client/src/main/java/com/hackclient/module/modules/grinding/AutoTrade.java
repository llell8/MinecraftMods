package com.hackclient.module.modules.grinding;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.NumberSetting;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.lwjgl.glfw.GLFW;

/**
 * When you open a villager's trade menu, does every trade you can afford until it sells out.
 * Great for trading halls (sticks, paper, crops -> emeralds).
 */
public class AutoTrade extends Module {
	private static final int RESULT_SLOT = 2;
	private static final int MAX_TRIES_PER_OFFER = 40;

	private final BoolSetting onlyEmeralds = bool("Only for emeralds", true);
	private final NumberSetting delay = number("Delay (ticks)", 1, 0, 10, 0);
	private final BoolSetting closeWhenDone = bool("Close when done", false);

	private int containerId = -1;
	private int offerIndex;
	private int tries;
	private boolean waitingForResult;
	private int timer;

	public AutoTrade() {
		super("AutoTrade", "Does all affordable villager trades automatically.", Category.GRINDING, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		if (!(mc.screen instanceof MerchantScreen screen)) {
			containerId = -1;
			return;
		}

		MerchantMenu menu = screen.getMenu();
		if (menu.containerId != containerId) {
			// New trade window opened - start from the first offer
			containerId = menu.containerId;
			offerIndex = 0;
			tries = 0;
			waitingForResult = false;
			timer = 5; // give the server a moment to send the offers
		}
		if (timer-- > 0) return;
		timer = delay.get().intValue();

		var offers = menu.getOffers();
		if (offerIndex >= offers.size()) {
			if (closeWhenDone.get() && !offers.isEmpty()) mc.player.closeContainer();
			return;
		}

		MerchantOffer offer = offers.get(offerIndex);
		if (offer.isOutOfStock() || (onlyEmeralds.get() && !offer.getResult().is(Items.EMERALD)) || tries >= MAX_TRIES_PER_OFFER) {
			nextOffer();
			return;
		}

		if (!waitingForResult) {
			// Same thing the vanilla screen does when you click a trade
			menu.setSelectionHint(offerIndex);
			menu.tryMoveItems(offerIndex);
			mc.getConnection().send(new ServerboundSelectTradePacket(offerIndex));
			waitingForResult = true;
			return;
		}

		waitingForResult = false;
		if (!menu.getSlot(RESULT_SLOT).hasItem()) {
			// Can't afford this one
			nextOffer();
			return;
		}
		mc.gameMode.handleInventoryMouseClick(menu.containerId, RESULT_SLOT, 0, ClickType.QUICK_MOVE, mc.player);
		tries++;
	}

	private void nextOffer() {
		offerIndex++;
		tries = 0;
		waitingForResult = false;
	}
}
