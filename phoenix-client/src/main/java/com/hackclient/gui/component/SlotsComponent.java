package com.hackclient.gui.component;

import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.setting.SlotsSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Name on top, then your 9 hotbar slots (showing what's in them). Click a slot to turn it on/off. */
public class SlotsComponent extends Component {
	private static final int BOX = 18;
	private static final int GAP = 2;
	private static final int HEIGHT = Theme.ROW + BOX + 3;

	private final SlotsSetting setting;

	public SlotsComponent(SlotsSetting setting) {
		this.setting = setting;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	@Override
	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		graphics.fill(x, y, x + width, y + HEIGHT, Theme.SETTING_BACKGROUND);
		graphics.drawString(font, Text.of(setting.getName()), x + Theme.PAD, Theme.textY(y, Theme.ROW), Theme.TEXT_DISABLED, true);

		Minecraft mc = Minecraft.getInstance();
		for (int i = 0; i < 9; i++) {
			int bx = boxX(i);
			int by = boxY();
			boolean enabled = setting.isEnabled(i);
			boolean hovered = mouseX >= bx && mouseX < bx + BOX && mouseY >= by && mouseY < by + BOX;

			// Border shows on/off, inside looks like a hotbar slot
			graphics.fill(bx, by, bx + BOX, by + BOX, enabled ? Theme.ACCENT : Theme.OUTLINE);
			graphics.fill(bx + 1, by + 1, bx + BOX - 1, by + BOX - 1, 0xFF1A1A1A);

			ItemStack stack = mc.player != null ? mc.player.getInventory().getItem(i) : ItemStack.EMPTY;
			if (!stack.isEmpty()) {
				graphics.renderItem(stack, bx + 1, by + 1);
			} else {
				String number = String.valueOf(i + 1);
				graphics.drawString(font, Text.of(number), bx + (BOX - font.width(Text.of(number))) / 2 + 1, by + (BOX - 8) / 2 + 1, Theme.TEXT_DIM, true);
			}

			if (!enabled) graphics.fill(bx + 1, by + 1, bx + BOX - 1, by + BOX - 1, 0xA0000000); // dim slots that are off
			if (hovered) graphics.fill(bx, by, bx + BOX, by + BOX, Theme.HOVER);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isHovered(mouseX, mouseY) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
		for (int i = 0; i < 9; i++) {
			int bx = boxX(i);
			if (mouseX >= bx && mouseX < bx + BOX && mouseY >= boxY() && mouseY < boxY() + BOX) {
				setting.toggle(i);
				return true;
			}
		}
		return true;
	}

	private int boxX(int index) {
		return x + Theme.PAD + index * (BOX + GAP);
	}

	private int boxY() {
		return y + Theme.ROW;
	}
}
