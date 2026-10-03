package com.hackclient.gui.component;

import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.module.Module;
import com.hackclient.module.modules.misc.ClickGui;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * Left-click, then press a key to bind (Esc/Delete/Backspace clears it).
 * Right-click unbinds straight away.
 */
public class BindComponent extends Component {
	private final Module module;
	private boolean listening;

	public BindComponent(Module module) {
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

		int textY = Theme.textY(y, Theme.ROW);
		graphics.drawString(font, Text.of("Bind"), x + Theme.PAD, textY, Theme.TEXT_DISABLED, true);
		String key = listening ? "Press a key..." : keyName(module.getKey());
		graphics.drawString(font, Text.of(key), x + width - font.width(Text.of(key)) - Theme.PAD, textY, listening ? Theme.ACCENT : Theme.TEXT, true);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isHovered(mouseX, mouseY)) {
			listening = false;
			return false;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			setKey(GLFW.GLFW_KEY_UNKNOWN);
			listening = false;
		} else {
			listening = !listening;
		}
		return true;
	}

	@Override
	public boolean keyPressed(int key) {
		if (!listening) return false;
		boolean clear = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE;
		setKey(clear ? GLFW.GLFW_KEY_UNKNOWN : key);
		listening = false;
		return true;
	}

	private void setKey(int key) {
		// The GUI is the only way to change binds, so it can't be left without one
		if (key == GLFW.GLFW_KEY_UNKNOWN && module instanceof ClickGui) key = GLFW.GLFW_KEY_RIGHT_SHIFT;
		module.setKey(key);
	}

	private static String keyName(int key) {
		if (key == GLFW.GLFW_KEY_UNKNOWN) return "None";
		return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
	}
}
