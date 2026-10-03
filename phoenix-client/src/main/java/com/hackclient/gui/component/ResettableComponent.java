package com.hackclient.gui.component;

import com.hackclient.gui.Theme;
import com.hackclient.setting.Setting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

/**
 * Wraps a setting's component and adds a small reset button on the right, like Meteor.
 * The button is dim while the setting is at its default and lights up once you change it.
 */
public class ResettableComponent extends Component {
	private static final int BUTTON_WIDTH = 12;

	// Circular "reset" arrow drawn pixel by pixel (Minecraft's font has no ↺ glyph)
	private static final String[] ICON = {
		"X.XXX..",
		"XX...X.",
		"XXX...X",
		"......X",
		"X.....X",
		".X...X.",
		"..XXX.."
	};

	private final Component inner;
	private final Setting<?> setting;

	public ResettableComponent(Component inner, Setting<?> setting) {
		this.inner = inner;
		this.setting = setting;
	}

	@Override
	public void setBounds(int x, int y, int width) {
		super.setBounds(x, y, width);
		inner.setBounds(x, y, width - BUTTON_WIDTH);
	}

	@Override
	public int getHeight() {
		return inner.getHeight();
	}

	@Override
	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		inner.render(graphics, font, mouseX, mouseY);

		int bx = buttonX();
		graphics.fill(bx, y, x + width, y + getHeight(), Theme.SETTING_BACKGROUND);
		if (isButtonHovered(mouseX, mouseY)) graphics.fill(bx, y, x + width, y + Theme.ROW, Theme.HOVER);

		int color = isDefault() ? 0xFF3A3A3A : Theme.ACCENT;
		drawIcon(graphics, bx + (BUTTON_WIDTH - ICON[0].length()) / 2, y + (Theme.ROW - ICON.length) / 2, color);
	}

	private static void drawIcon(GuiGraphics graphics, int left, int top, int color) {
		for (int row = 0; row < ICON.length; row++) {
			for (int col = 0; col < ICON[row].length(); col++) {
				if (ICON[row].charAt(col) == 'X') graphics.fill(left + col, top + row, left + col + 1, top + row + 1, color);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isButtonHovered(mouseX, mouseY)) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) setting.reset();
			return true;
		}
		return inner.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void mouseReleased() {
		inner.mouseReleased();
	}

	@Override
	public void mouseDragged(double mouseX, double mouseY) {
		inner.mouseDragged(mouseX, mouseY);
	}

	@Override
	public boolean keyPressed(int key) {
		return inner.keyPressed(key);
	}

	private boolean isDefault() {
		return Objects.equals(setting.get(), setting.getDefault());
	}

	private int buttonX() {
		return x + width - BUTTON_WIDTH;
	}

	private boolean isButtonHovered(double mouseX, double mouseY) {
		return mouseX >= buttonX() && mouseX < x + width && mouseY >= y && mouseY < y + Theme.ROW;
	}
}
