package com.hackclient.hud;

import com.hackclient.HackClient;
import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.module.modules.render.PlayerTracker;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** Draws PlayerTracker's list in the top-left corner, under the client name. */
public class PlayerTrackerHud implements HudElement {
	@Override
	public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft mc = Minecraft.getInstance();
		PlayerTracker tracker = HackClient.getModuleManager().getIfEnabled(PlayerTracker.class);
		if (tracker == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;

		Font font = mc.font;
		List<PlayerTracker.Entry> entries = tracker.entries();
		int y = 2 + font.lineHeight + 3;
		String header = entries.isEmpty() ? "No players nearby" : "Players (" + entries.size() + ")";
		graphics.drawString(font, Text.of(header), 2, y, Theme.TEXT_DIM, true);
		y += font.lineHeight + 1;

		for (PlayerTracker.Entry entry : entries) {
			int width = font.width(Text.of(entry.text()));
			graphics.fill(0, y - 1, width + 4, y + font.lineHeight, 0x80000000);
			graphics.fill(0, y - 1, 1, y + font.lineHeight, Theme.ACCENT);
			graphics.drawString(font, Text.of(entry.text()), 3, y, colorFor(entry.distance()), true);
			y += font.lineHeight + 1;
		}
	}

	/** Red when close, yellow at medium range, green when far. */
	private static int colorFor(double distance) {
		if (distance < 16) return 0xFFFF5555;
		if (distance < 48) return 0xFFFFFF55;
		return 0xFF55FF55;
	}
}
