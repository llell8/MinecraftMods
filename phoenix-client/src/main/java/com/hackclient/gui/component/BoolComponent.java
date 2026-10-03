package com.hackclient.gui.component;

import com.hackclient.gui.Pixels;
import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.setting.BoolSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class BoolComponent extends Component {
	private final BoolSetting setting;

	public BoolComponent(BoolSetting setting) {
		this.setting = setting;
	}

	@Override
	public int getHeight() {
		return Theme.ROW;
	}

	@Override
	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		graphics.fill(x, y, x + width, y + Theme.ROW, Theme.SETTING_BACKGROUND);
		if (isHovered(mouseX, mouseY)) graphics.fill(x, y, x + width, y + Theme.ROW, Theme.HOVER);

		graphics.drawString(font, Text.of(setting.getName()), x + Theme.PAD, Theme.textY(y, Theme.ROW), Theme.TEXT_DISABLED, true);
		drawToggle(graphics, x + width - Theme.PAD, y, Theme.ROW, setting.get());
	}

	/** Draws an on/off control in the chosen checkbox style, right-aligned to {@code right}, centered in the row. */
	static void drawToggle(GuiGraphics graphics, int right, int rowY, int rowHeight, boolean on) {
		int centerY = rowY + rowHeight / 2;
		switch (Theme.checkboxStyle) {
			case BOX -> {
				int size = 9;
				int bx = right - size;
				int by = centerY - size / 2;
				graphics.fill(bx, by, bx + size, by + size, on ? Theme.ACCENT : Theme.OUTLINE);
				graphics.fill(bx + 1, by + 1, bx + size - 1, by + size - 1, on ? Theme.ACCENT : Theme.SETTING_BACKGROUND);
				if (on) graphics.fill(bx + 3, by + 3, bx + size - 3, by + size - 3, Theme.TEXT);
			}
			case SWITCH -> {
				int w = 16;
				int h = 8;
				int sx = right - w;
				int sy = centerY - h / 2;
				Pixels.roundedRect(graphics, sx, sy, sx + w, sy + h, on ? Theme.ACCENT : Theme.OUTLINE);
				int knobX = on ? sx + w - 7 : sx + 1;
				Pixels.roundedRect(graphics, knobX, sy + 1, knobX + 6, sy + h - 1, Theme.TEXT);
			}
			case CHECK -> {
				int size = 9;
				int bx = right - size;
				int by = centerY - size / 2;
				graphics.fill(bx, by, bx + size, by + size, on ? Theme.ACCENT : Theme.OUTLINE);
				graphics.fill(bx + 1, by + 1, bx + size - 1, by + size - 1, on ? Theme.ACCENT : Theme.SETTING_BACKGROUND);
				if (on) Pixels.draw(graphics, Pixels.CHECKMARK, bx + 1, by + 2, Theme.TEXT);
			}
			case DOT -> {
				int bx = right - 9;
				int by = centerY - 4;
				Pixels.draw(graphics, Pixels.RING, bx, by, on ? Theme.ACCENT : Theme.TEXT_DIM);
				if (on) Pixels.draw(graphics, Pixels.DOT, bx + 2, by + 2, Theme.ACCENT);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isHovered(mouseX, mouseY) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
		setting.toggle();
		return true;
	}
}
