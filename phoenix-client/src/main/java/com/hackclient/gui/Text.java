package com.hackclient.gui;

import com.hackclient.HackClient;
import com.hackclient.module.modules.misc.ClickGui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Wraps GUI/HUD text in the font picked in ClickGUI's "Font" setting.
 * Use Text.of(...) wherever text is drawn or measured so the font applies everywhere.
 *
 * TrueType fonts are rasterized at a fixed resolution, and stretching that to a different on-screen
 * size drops or doubles pixel rows (the "holes"). So each font ships one definition per on-screen
 * pixel size (font/<name>_<pixels*2>.json), and we pick the one matching how big we're drawing.
 */
public final class Text {
	private static final int MIN_HALF_PIXELS = 2;  // 1x
	private static final int MAX_HALF_PIXELS = 16; // 8x

	private static FontChoice current = FontChoice.MINECRAFT;
	private static double pixelScale = 2;
	private static Style style = Style.EMPTY;

	private Text() {}

	/** Pulls the current font from the ClickGUI module. Called from Theme.update() once per frame. */
	static void update() {
		ClickGui clickGui = HackClient.getModuleManager() != null ? HackClient.getModuleManager().get(ClickGui.class) : null;
		FontChoice choice = clickGui != null ? clickGui.getFont() : FontChoice.MINECRAFT;
		if (choice != current) {
			current = choice;
			rebuildStyle();
		}
	}

	/** How many real screen pixels one GUI unit covers right now (Minecraft GUI scale × our scale). */
	public static void setPixelScale(double scale) {
		if (scale != pixelScale) {
			pixelScale = scale;
			rebuildStyle();
		}
	}

	private static void rebuildStyle() {
		if (current.id == null) {
			style = Style.EMPTY;
			return;
		}
		int halfPixels = (int) Math.clamp(Math.round(pixelScale * 2), MIN_HALF_PIXELS, MAX_HALF_PIXELS);
		Identifier id = Identifier.fromNamespaceAndPath(HackClient.MOD_ID, current.id + "_" + halfPixels);
		style = Style.EMPTY.withFont(new FontDescription.Resource(id));
	}

	public static FontChoice current() {
		return current;
	}

	public static Component of(String text) {
		return Component.literal(text).withStyle(style);
	}

	/**
	 * Bold text for titles. Minecraft fakes bold by drawing each glyph twice, 1 pixel apart: fine for the
	 * pixel font, but smooth fonts end up looking hollow. So only the Minecraft font gets bold.
	 */
	public static Component bold(String text) {
		Component component = of(text);
		return current.id == null ? component.copy().withStyle(ChatFormatting.BOLD) : component;
	}

	/** Keeps the component's own styling (e.g. bold) and adds our font on top. */
	public static Component of(Component component) {
		return component.copy().withStyle(style);
	}

	/** Cuts the text so it fits in maxWidth when drawn in the current font. */
	public static String trim(Font font, String text, int maxWidth) {
		if (font.width(of(text)) <= maxWidth) return text;
		String trimmed = text;
		while (!trimmed.isEmpty() && font.width(of(trimmed + "...")) > maxWidth) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed + "...";
	}
}
