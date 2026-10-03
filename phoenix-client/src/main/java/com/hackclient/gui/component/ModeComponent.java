package com.hackclient.gui.component;

import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.setting.ModeSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** Left-click: next option, right-click: previous option. */
public class ModeComponent extends Component {
	private final ModeSetting<?> setting;

	public ModeComponent(ModeSetting<?> setting) {
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

		int textY = Theme.textY(y, Theme.ROW);
		graphics.drawString(font, Text.of(setting.getName()), x + Theme.PAD, textY, Theme.TEXT_DISABLED, true);

		String value = "< " + setting.get() + " >";
		graphics.drawString(font, Text.of(value), x + width - font.width(Text.of(value)) - Theme.PAD, textY, Theme.ACCENT, true);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isHovered(mouseX, mouseY)) return false;
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) setting.cycle(true);
		else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) setting.cycle(false);
		return true;
	}
}
