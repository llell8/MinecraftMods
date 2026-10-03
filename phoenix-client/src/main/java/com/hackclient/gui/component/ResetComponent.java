package com.hackclient.gui.component;

import com.hackclient.HackClient;
import com.hackclient.config.Config;
import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.module.Module;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** Resets a module's settings. Needs two clicks within a few seconds so you can't do it by accident. */
public class ResetComponent extends Component {
	private static final long CONFIRM_MILLIS = 3000;

	private final Module module;
	private long armedAt;

	public ResetComponent(Module module) {
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

		boolean armed = isArmed();
		String text = armed ? "Click again to reset everything" : "Reset all to default";
		graphics.drawString(font, Text.of(text), x + (width - font.width(Text.of(text))) / 2, Theme.textY(y, Theme.ROW), armed ? Theme.RED : Theme.TEXT_DIM, true);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isHovered(mouseX, mouseY)) {
			armedAt = 0; // clicking anywhere else cancels
			return false;
		}
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;

		if (isArmed()) {
			module.resetSettings();
			Config.save(HackClient.getModuleManager());
			armedAt = 0;
		} else {
			armedAt = System.currentTimeMillis();
		}
		return true;
	}

	private boolean isArmed() {
		return armedAt != 0 && System.currentTimeMillis() - armedAt < CONFIRM_MILLIS;
	}
}
