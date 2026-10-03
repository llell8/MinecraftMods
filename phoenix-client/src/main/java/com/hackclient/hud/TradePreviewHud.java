package com.hackclient.hud;

import com.hackclient.HackClient;
import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.module.modules.grinding.TradePreview;
import com.hackclient.util.FutureTrades;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the trade list of the villager under the crosshair, to the right of the crosshair:
 * its current trades, then the pool of trades it could get at the next level(s).
 */
public class TradePreviewHud implements HudElement {
	private static final String[] LEVELS = {"Novice", "Apprentice", "Journeyman", "Expert", "Master"};
	private static final int ROW = 18;
	private static final int HEADER = 13;
	private static final int WIDTH = 210;
	private static final int GOLD = 0xFFFFAA00;
	private static final int SOLD_OUT = 0xFFFF5555;

	/** One line of the panel: either a section header or a trade row. */
	private record Line(String header, int headerColor, ItemStack costA, ItemStack costB, ItemStack result, String label, int color) {
		static Line header(String text, int color) {
			return new Line(text, color, null, null, null, null, 0);
		}

		static Line trade(ItemStack costA, ItemStack costB, ItemStack result, String label, int color) {
			return new Line(null, 0, costA, costB, result, label, color);
		}

		int height() {
			return header != null ? HEADER : ROW;
		}
	}

	@Override
	public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Theme.update();
		Text.setPixelScale(Minecraft.getInstance().getWindow().getGuiScale());
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.screen != null) return;

		TradePreview module = HackClient.getModuleManager().getIfEnabled(TradePreview.class);
		if (module == null || !(mc.crosshairPickEntity instanceof Villager villager)) return;
		TradePreview.CachedTrades trades = module.getTrades(villager);
		if (trades == null) return;

		List<Line> lines = buildLines(villager, trades, module.getFutureLevels());

		Font font = mc.font;
		int height = lines.stream().mapToInt(Line::height).sum() + 4;
		int x = graphics.guiWidth() / 2 + 14;
		int y = Math.max(2, graphics.guiHeight() / 2 - height / 2);

		graphics.fill(x, y, x + WIDTH, y + height, Theme.BACKGROUND);
		graphics.fill(x, y, x + 2, y + height, Theme.ACCENT);

		int lineY = y + 2;
		for (Line line : lines) {
			if (line.header() != null) {
				graphics.drawString(font, Text.of(line.header()), x + 6, lineY + 2, line.headerColor(), false);
			} else {
				drawTrade(graphics, font, line, x + 6, lineY);
			}
			lineY += line.height();
		}
	}

	private static List<Line> buildLines(Villager villager, TradePreview.CachedTrades trades, int futureLevels) {
		List<Line> lines = new ArrayList<>();
		int level = Math.clamp(trades.level(), 1, LEVELS.length);

		lines.add(Line.header(capitalize(trades.profession()) + " - " + LEVELS[level - 1], Theme.ACCENT));
		for (MerchantOffer offer : trades.offers()) {
			String label = label(offer.getResult());
			int color = TradePreview.hasMaxLevelBook(offer.getResult()) ? GOLD : Theme.TEXT;
			if (offer.isOutOfStock()) {
				label += " (sold out)";
				color = SOLD_OUT;
			}
			lines.add(Line.trade(offer.getCostA(), offer.getCostB(), offer.getResult(), label, color));
		}

		for (int next = level + 1; next <= Math.min(LEVELS.length, level + futureLevels); next++) {
			List<FutureTrades.PossibleTrade> pool = FutureTrades.forLevel(villager, next);
			if (pool.isEmpty()) continue;

			int picks = Math.min(FutureTrades.PICKS_PER_LEVEL, FutureTrades.poolSize(villager, next));
			String header = LEVELS[next - 1] + (pool.size() > picks ? " - gets " + picks + " of:" : " - gets:");
			lines.add(Line.header(header, Theme.TEXT_DIM));
			for (FutureTrades.PossibleTrade trade : pool) {
				String label = trade.label() != null ? trade.label() : label(trade.result());
				lines.add(Line.trade(trade.costA(), trade.costB(), trade.result(), label, Theme.TEXT_DISABLED));
			}
		}
		return lines;
	}

	private static void drawTrade(GuiGraphics graphics, Font font, Line line, int x, int y) {
		drawItem(graphics, font, line.costA(), x, y);
		if (!line.costB().isEmpty()) drawItem(graphics, font, line.costB(), x + 17, y);

		graphics.drawString(font, Text.of(">"), x + 37, y + 5, Theme.TEXT_DIM, false);
		drawItem(graphics, font, line.result(), x + 46, y);

		graphics.drawString(font, Text.of(Text.trim(font, line.label(), WIDTH - 72)), x + 66, y + 5, line.color(), false);
	}

	private static void drawItem(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
		graphics.renderItem(stack, x, y);
		graphics.renderItemDecorations(font, stack, x, y);
	}

	/** Enchanted books show their enchantments, everything else shows the item name. */
	private static String label(ItemStack stack) {
		ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
		if (enchantments != null && !enchantments.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
				if (!builder.isEmpty()) builder.append(", ");
				builder.append(Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString());
			}
			return builder.toString();
		}
		return stack.getHoverName().getString();
	}

	private static String capitalize(String text) {
		return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1).replace('_', ' ');
	}
}
