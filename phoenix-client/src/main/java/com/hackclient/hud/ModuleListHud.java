package com.hackclient.hud;

import com.hackclient.HackClient;
import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Comparator;
import java.util.List;

/** Draws the client name and a list of enabled modules in the top-right corner. */
public class ModuleListHud implements HudElement {
	@Override
	public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Theme.update();
		Text.setPixelScale(Minecraft.getInstance().getWindow().getGuiScale());
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;

		Font font = mc.font;
		graphics.drawString(font, Text.of(HackClient.NAME), 2, 2, Theme.ACCENT, true);

		List<Module> enabled = HackClient.getModuleManager().getModules().stream()
				.filter(Module::isEnabled)
				.sorted(Comparator.comparingInt((Module m) -> font.width(Text.of(m.getName()))).reversed())
				.toList();

		int screenWidth = graphics.guiWidth();
		int y = 2;
		for (Module module : enabled) {
			int textWidth = font.width(Text.of(module.getName()));
			int x = screenWidth - textWidth - 4;
			graphics.fill(x - 2, y - 1, screenWidth, y + font.lineHeight, 0x80000000);
			graphics.fill(screenWidth - 1, y - 1, screenWidth, y + font.lineHeight, Theme.ACCENT);
			graphics.drawString(font, Text.of(module.getName()), x, y, Theme.TEXT, true);
			y += font.lineHeight + 1;
		}
	}
}
