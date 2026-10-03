package com.hackclient.gui;

import net.minecraft.client.gui.GuiGraphics;

/** Tiny pixel-art shapes for widgets, drawn from text patterns ('X' = pixel). */
public final class Pixels {
	public static final String[] CHECKMARK = {
		"......X",
		".....XX",
		"X...XX.",
		"XX.XX..",
		".XXX...",
		"..X...."
	};

	public static final String[] RING = {
		"...XXX...",
		".XX...XX.",
		".X.....X.",
		"X.......X",
		"X.......X",
		"X.......X",
		".X.....X.",
		".XX...XX.",
		"...XXX..."
	};

	public static final String[] DOT = {
		".XXX.",
		"XXXXX",
		"XXXXX",
		"XXXXX",
		".XXX."
	};

	public static final String[] VERTICAL_DOTS = {
		"XX",
		"XX",
		"..",
		"XX",
		"XX",
		"..",
		"XX",
		"XX"
	};

	public static final String[] ARROW = {
		"X...",
		".X..",
		"..X.",
		"...X",
		"..X.",
		".X..",
		"X..."
	};

	public static final String[] GEAR = {
		"...X...",
		".XXXXX.",
		".XX.XX.",
		"XX...XX",
		".XX.XX.",
		".XXXXX.",
		"...X..."
	};

	public static final String[] LINES = {
		"XXXXXXX",
		".......",
		"XXXXXXX",
		".......",
		"XXXXXXX"
	};

	private Pixels() {}

	public static void draw(GuiGraphics graphics, String[] pattern, int x, int y, int color) {
		for (int row = 0; row < pattern.length; row++) {
			for (int col = 0; col < pattern[row].length(); col++) {
				if (pattern[row].charAt(col) == 'X') graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
			}
		}
	}

	/** A rectangle with its four corner pixels left out, so it looks slightly rounded. */
	public static void roundedRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
		graphics.fill(x1 + 1, y1, x2 - 1, y2, color);
		graphics.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
		graphics.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
	}
}
