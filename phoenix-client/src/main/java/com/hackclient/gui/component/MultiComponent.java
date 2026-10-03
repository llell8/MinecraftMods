package com.hackclient.gui.component;

import com.hackclient.gui.MultiSelectScreen;
import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.setting.HasIcon;
import com.hackclient.setting.MultiSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * Multi-choice setting.
 * With item icons (like Meteor's item lists): shows the selected items' pictures; click to open a picker window.
 * Without icons: a wrapping row of clickable chips.
 */
public class MultiComponent extends Component {
	private static final int CHIP_HEIGHT = 12;
	private static final int ICON = 17;
	private static final int GAP = 3;
	private static final int DEFAULT_WIDTH = 200; // used before the first layout pass sets our width

	private final MultiSetting<?> setting;

	public MultiComponent(MultiSetting<?> setting) {
		this.setting = setting;
	}

	@Override
	public int getHeight() {
		if (setting.hasIcons()) {
			int perRow = Math.max(1, (rowWidth() - Theme.PAD * 2) / ICON);
			int rows = Math.max(1, (selectedCount() + perRow - 1) / perRow);
			return Theme.ROW + rows * ICON + 2;
		}
		Font font = Minecraft.getInstance().font;
		int lastBottom = 0;
		for (Enum<?> value : setting.getValues()) lastBottom = chipBounds(font, value)[3];
		return lastBottom - y + 6;
	}

	@Override
	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		int height = getHeight();
		graphics.fill(x, y, x + width, y + height, Theme.SETTING_BACKGROUND);

		if (setting.hasIcons()) {
			renderIcons(graphics, font, mouseX, mouseY, height);
			return;
		}

		graphics.drawString(font, Text.of(setting.getName()), x + Theme.PAD, Theme.textY(y, Theme.ROW), Theme.TEXT_DISABLED, true);
		for (Enum<?> value : setting.getValues()) {
			int[] b = chipBounds(font, value);
			boolean enabled = isEnabled(value);
			graphics.fill(b[0], b[1], b[2], b[3], enabled ? Theme.ACCENT : Theme.OUTLINE);
			if (mouseX >= b[0] && mouseX < b[2] && mouseY >= b[1] && mouseY < b[3]) graphics.fill(b[0], b[1], b[2], b[3], Theme.HOVER);
			graphics.drawString(font, Text.of(value.toString()), b[0] + 4, b[1] + (CHIP_HEIGHT - 8) / 2 + 1, enabled ? Theme.TEXT : Theme.TEXT_DIM, true);
		}
	}

	private void renderIcons(GuiGraphics graphics, Font font, int mouseX, int mouseY, int height) {
		boolean hovered = isHovered(mouseX, mouseY);
		if (hovered) graphics.fill(x, y, x + width, y + height, Theme.HOVER);

		graphics.drawString(font, Text.of(setting.getName()), x + Theme.PAD, Theme.textY(y, Theme.ROW), Theme.TEXT_DISABLED, true);
		String edit = hovered ? "Edit >" : selectedCount() + "/" + setting.getValues().length;
		graphics.drawString(font, Text.of(edit), x + width - font.width(Text.of(edit)) - Theme.PAD, Theme.textY(y, Theme.ROW), hovered ? Theme.ACCENT : Theme.TEXT_DIM, true);

		int perRow = Math.max(1, (width - Theme.PAD * 2) / ICON);
		int i = 0;
		for (Enum<?> value : setting.getValues()) {
			if (!isEnabled(value)) continue;
			int ix = x + Theme.PAD + (i % perRow) * ICON;
			int iy = y + Theme.ROW + (i / perRow) * ICON;
			graphics.renderItem(((HasIcon) value).icon(), ix, iy);
			i++;
		}
		if (i == 0) graphics.drawString(font, Text.of("Nothing selected"), x + Theme.PAD, y + Theme.ROW + 4, Theme.RED, true);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isHovered(mouseX, mouseY) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

		if (setting.hasIcons()) {
			Minecraft mc = Minecraft.getInstance();
			mc.setScreen(new MultiSelectScreen(setting, mc.screen));
			return true;
		}

		Font font = Minecraft.getInstance().font;
		for (Enum<?> value : setting.getValues()) {
			int[] b = chipBounds(font, value);
			if (mouseX >= b[0] && mouseX < b[2] && mouseY >= b[1] && mouseY < b[3]) {
				toggle(value);
				return true;
			}
		}
		return true;
	}

	private int rowWidth() {
		return width > 0 ? width : DEFAULT_WIDTH;
	}

	private int selectedCount() {
		int count = 0;
		for (Enum<?> value : setting.getValues()) if (isEnabled(value)) count++;
		return count;
	}

	/** Flow layout: {left, top, right, bottom} of the chip for this option. */
	private int[] chipBounds(Font font, Enum<?> target) {
		int chipX = x + Theme.PAD;
		int chipY = y + Theme.ROW;
		for (Enum<?> value : setting.getValues()) {
			int chipWidth = font.width(Text.of(value.toString())) + 8;
			if (chipX + chipWidth > x + rowWidth() - 6 && chipX > x + Theme.PAD) {
				chipX = x + Theme.PAD;
				chipY += CHIP_HEIGHT + GAP;
			}
			if (value == target) return new int[] {chipX, chipY, chipX + chipWidth, chipY + CHIP_HEIGHT};
			chipX += chipWidth + GAP;
		}
		return new int[] {x, y, x, y};
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private boolean isEnabled(Enum<?> value) {
		return ((MultiSetting) setting).isEnabled(value);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void toggle(Enum<?> value) {
		((MultiSetting) setting).toggle(value);
	}
}
