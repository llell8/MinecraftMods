package com.hackclient.gui.component;

import com.hackclient.gui.Pixels;
import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.module.Module;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** A module row in a category panel. Left-click toggles; right-click is handled by the screen (opens settings). */
public class ModuleButton extends Component {
	private final Module module;

	public ModuleButton(Module module) {
		this.module = module;
	}

	public Module getModule() {
		return module;
	}

	@Override
	public int getHeight() {
		return Theme.MODULE_ROW;
	}

	@Override
	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		boolean enabled = module.isEnabled();
		int textColor = enabled ? Theme.TEXT : Theme.TEXT_DISABLED;
		if (enabled) {
			switch (Theme.moduleStyle) {
				case BAR -> {
					graphics.fill(x, y, x + width, y + Theme.MODULE_ROW, Theme.ACCENT_FADED);
					graphics.fill(x, y, x + 2, y + Theme.MODULE_ROW, Theme.ACCENT);
				}
				case FILL -> graphics.fill(x, y, x + width, y + Theme.MODULE_ROW, Theme.ACCENT_DARK);
				case TEXT -> textColor = Theme.ACCENT;
			}
		}
		if (isHovered(mouseX, mouseY)) graphics.fill(x, y, x + width, y + Theme.MODULE_ROW, Theme.HOVER);

		graphics.drawString(font, Text.of(module.getName()), x + Theme.PAD, Theme.textY(y, Theme.MODULE_ROW), textColor, true);

		// Small icon on the right for modules that have settings (ClickGUI → Buttons → Settings icon)
		if (!module.getSettings().isEmpty()) {
			String[] icon = switch (Theme.settingsIcon) {
				case DOTS -> Pixels.VERTICAL_DOTS;
				case ARROW -> Pixels.ARROW;
				case GEAR -> Pixels.GEAR;
				case LINES -> Pixels.LINES;
				case NONE -> null;
			};
			if (icon != null) {
				int iconX = x + width - Theme.PAD - icon[0].length();
				int iconY = y + (Theme.MODULE_ROW - icon.length) / 2;
				Pixels.draw(graphics, icon, iconX, iconY, isHovered(mouseX, mouseY) ? Theme.ACCENT : Theme.TEXT_DIM);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isHovered(mouseX, mouseY)) return false;
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) module.toggle();
		return true;
	}
}
