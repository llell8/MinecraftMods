package com.hackclient.gui.component;

import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.setting.ColorSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Meteor-style colour setting: a row with the colour swatch; click it to open hue, saturation,
 * brightness and alpha sliders, each drawn as a gradient of what you'd get.
 */
public class ColorComponent extends Component {
	private static final int SWATCH = 16;
	private static final int SEGMENTS = 32;
	private static final String[] LABELS = {"Hue", "Sat", "Bri", "Alpha"};

	private final ColorSetting setting;
	private boolean open;
	private int dragging = -1;
	// Kept separately so hue survives turning saturation or brightness down to zero
	private float hue, saturation, brightness;
	private int lastValue;

	public ColorComponent(ColorSetting setting) {
		this.setting = setting;
		syncFromSetting();
	}

	private void syncFromSetting() {
		int argb = setting.get();
		float[] hsb = java.awt.Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
		if (hsb[1] > 0 && hsb[2] > 0) hue = hsb[0];
		saturation = hsb[1];
		brightness = hsb[2];
		lastValue = argb;
	}

	private int alpha() {
		return (setting.get() >>> 24) & 0xFF;
	}

	private void apply(float h, float s, float b, int a) {
		hue = h;
		saturation = s;
		brightness = b;
		int rgb = Mth.hsvToRgb(h, s, b) & 0x00FFFFFF;
		setting.set((a << 24) | rgb);
		lastValue = setting.get();
	}

	@Override
	public int getHeight() {
		return Theme.ROW * (open ? 5 : 1);
	}

	@Override
	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		if (setting.get() != lastValue) syncFromSetting(); // reset button or config load
		graphics.fill(x, y, x + width, y + getHeight(), Theme.SETTING_BACKGROUND);
		if (mouseY >= y && mouseY < y + Theme.ROW && isHovered(mouseX, mouseY)) graphics.fill(x, y, x + width, y + Theme.ROW, Theme.HOVER);

		int textY = Theme.textY(y, Theme.ROW);
		graphics.drawString(font, Text.of(setting.getName()), x + Theme.PAD, textY, Theme.TEXT_DISABLED, true);

		// Swatch over a checkerboard, so transparency is visible
		int sx = x + width - Theme.PAD - SWATCH;
		int sy = y + (Theme.ROW - 9) / 2;
		for (int i = 0; i < SWATCH; i += 4) {
			for (int j = 0; j < 9; j += 3) {
				graphics.fill(sx + i, sy + j, sx + Math.min(i + 4, SWATCH), sy + Math.min(j + 3, 9), ((i / 4 + j / 3) % 2 == 0) ? 0xFFCCCCCC : 0xFF888888);
			}
		}
		graphics.fill(sx, sy, sx + SWATCH, sy + 9, setting.get());
		graphics.fill(sx - 1, sy - 1, sx + SWATCH + 1, sy, Theme.OUTLINE);
		graphics.fill(sx - 1, sy + 9, sx + SWATCH + 1, sy + 10, Theme.OUTLINE);

		if (!open) return;
		for (int row = 0; row < 4; row++) {
			int ry = y + Theme.ROW * (row + 1);
			graphics.drawString(font, Text.of(LABELS[row]), x + Theme.PAD + 4, Theme.textY(ry, Theme.ROW), Theme.TEXT_DIM, true);
			int left = trackLeft(), right = trackRight();
			int top = ry + 3, bottom = ry + Theme.ROW - 3;
			for (int s = 0; s < SEGMENTS; s++) {
				float t = (s + 0.5f) / SEGMENTS;
				int color = switch (row) {
					case 0 -> 0xFF000000 | Mth.hsvToRgb(t, 1, 1);
					case 1 -> 0xFF000000 | Mth.hsvToRgb(hue, t, brightness);
					case 2 -> 0xFF000000 | Mth.hsvToRgb(hue, saturation, t);
					default -> ((int) (t * 255) << 24) | (setting.get() & 0x00FFFFFF);
				};
				int a = left + (right - left) * s / SEGMENTS;
				int b = left + (right - left) * (s + 1) / SEGMENTS;
				graphics.fill(a, top, b, bottom, color);
			}
			float value = switch (row) {
				case 0 -> hue;
				case 1 -> saturation;
				case 2 -> brightness;
				default -> alpha() / 255f;
			};
			int kx = left + (int) ((right - left) * value);
			graphics.fill(kx - 1, top - 1, kx + 1, bottom + 1, Theme.TEXT);
		}
	}

	private int trackLeft() {
		return x + Theme.PAD + 34;
	}

	private int trackRight() {
		return x + width - Theme.PAD;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isHovered(mouseX, mouseY)) return false;
		if (mouseY < y + Theme.ROW) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) open = !open;
			return true;
		}
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
		dragging = (int) ((mouseY - y) / Theme.ROW) - 1;
		update(mouseX);
		return true;
	}

	@Override
	public void mouseDragged(double mouseX, double mouseY) {
		if (dragging >= 0) update(mouseX);
	}

	@Override
	public void mouseReleased() {
		dragging = -1;
	}

	private void update(double mouseX) {
		float t = (float) Math.clamp((mouseX - trackLeft()) / (double) (trackRight() - trackLeft()), 0.0, 1.0);
		switch (dragging) {
			case 0 -> apply(t, saturation, brightness, alpha());
			case 1 -> apply(hue, t, brightness, alpha());
			case 2 -> apply(hue, saturation, t, alpha());
			case 3 -> apply(hue, saturation, brightness, Math.round(t * 255));
			default -> {
			}
		}
	}
}
