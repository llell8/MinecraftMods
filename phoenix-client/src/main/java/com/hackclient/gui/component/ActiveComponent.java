package com.hackclient.gui.component;

import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.module.Module;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** "Active" row at the top of a module's settings window: turns the module on/off. */
public class ActiveComponent extends Component {
	private final Module module;

	public ActiveComponent(Module module) {
		this.module = module;
	}

	@Override
	public int getHeight() {
		return Theme.ROW;
	}

	@Override
	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		graphics.fill(x, y, x + width, y + Theme.ROW, Theme.SETTING_BACKGROUND);
		if (isHovered(mouseX, mouseY)) graphics.fill(x, y, x + width, y + Theme.ROW, Theme.HOVER);

		graphics.drawString(font, Text.of("Active"), x + Theme.PAD, Theme.textY(y, Theme.ROW), Theme.TEXT, true);
		BoolComponent.drawToggle(graphics, x + width - Theme.PAD, y, Theme.ROW, module.isEnabled());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isHovered(mouseX, mouseY) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
		module.toggle();
		return true;
	}
}
