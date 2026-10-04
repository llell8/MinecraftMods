package com.hackclient.render;

import net.minecraft.client.gui.GuiGraphics;

/** Simple 2D shapes made from fills, for the overlay. Coordinates are real screen pixels. */
public final class Draw {
	private Draw() {}

	public static void line(GuiGraphics g, double x0, double y0, double x1, double y1, int color, int thickness) {
		double dx = x1 - x0, dy = y1 - y0;
		int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)));
		if (steps > 5000) {
			// Clip absurdly long lines (far off screen) to keep it cheap
			double scale = 5000.0 / steps;
			dx *= scale;
			dy *= scale;
			steps = 5000;
		}
		if (steps == 0) return;
		// Draw runs of pixels rather than single pixels where the line is mostly straight
		double sx = dx / steps, sy = dy / steps;
		boolean horizontal = Math.abs(dx) >= Math.abs(dy);
		int runStart = 0;
		int lastX = (int) Math.round(x0), lastY = (int) Math.round(y0);
		for (int i = 1; i <= steps; i++) {
			int px = (int) Math.round(x0 + sx * i), py = (int) Math.round(y0 + sy * i);
			boolean breaks = horizontal ? py != lastY : px != lastX;
			if (breaks || i == steps) {
				int ax = (int) Math.round(x0 + sx * runStart), ay = (int) Math.round(y0 + sy * runStart);
				int bx = horizontal ? (int) Math.round(x0 + sx * (i - 1)) : ax;
				int by = horizontal ? ay : (int) Math.round(y0 + sy * (i - 1));
				if (horizontal) g.fill(Math.min(ax, bx), ay, Math.max(ax, bx) + 1, ay + thickness, color);
				else g.fill(ax, Math.min(ay, by), ax + thickness, Math.max(ay, by) + 1, color);
				runStart = i;
			}
			lastX = px;
			lastY = py;
		}
	}

	public static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int color, int thickness) {
		g.fill(x1, y1, x2, y1 + thickness, color);
		g.fill(x1, y2 - thickness, x2, y2, color);
		g.fill(x1, y1, x1 + thickness, y2, color);
		g.fill(x2 - thickness, y1, x2, y2, color);
	}
}
