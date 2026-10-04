package com.hackclient.gui.component;

import com.hackclient.gui.BlockSelectScreen;
import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.setting.BlockListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;

/** Shows the selected blocks' icons; click to open the block picker. */
public class BlockListComponent extends Component {
	private static final int ICON = 17;
	private static final int MAX_ROWS = 2;

	private final BlockListSetting setting;

	public BlockListComponent(BlockListSetting setting) {
		this.setting = setting;
	}

	@Override
	public int getHeight() {
		return Theme.ROW + MAX_ROWS * ICON + 2;
	}

	@Override
	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		int height = getHeight();
		boolean hovered = isHovered(mouseX, mouseY);
		graphics.fill(x, y, x + width, y + height, Theme.SETTING_BACKGROUND);
		if (hovered) graphics.fill(x, y, x + width, y + height, Theme.HOVER);

		graphics.drawString(font, Text.of(setting.getName()), x + Theme.PAD, Theme.textY(y, Theme.ROW), Theme.TEXT_DISABLED, true);
		String edit = hovered ? "Edit >" : setting.get().size() + " selected";
		graphics.drawString(font, Text.of(edit), x + width - font.width(Text.of(edit)) - Theme.PAD, Theme.textY(y, Theme.ROW), hovered ? Theme.ACCENT : Theme.TEXT_DIM, true);

		int perRow = Math.max(1, (width - Theme.PAD * 2) / ICON);
		int shown = 0;
		for (Block block : BuiltInRegistries.BLOCK) {
			if (!setting.contains(block)) continue;
			ItemStack icon = new ItemStack(block.asItem());
			if (icon.isEmpty()) continue;
			if (shown >= perRow * MAX_ROWS) break;
			graphics.renderItem(icon, x + Theme.PAD + (shown % perRow) * ICON, y + Theme.ROW + (shown / perRow) * ICON);
			shown++;
		}
		if (setting.isEmpty()) graphics.drawString(font, Text.of("Nothing selected"), x + Theme.PAD, y + Theme.ROW + 4, Theme.RED, true);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isHovered(mouseX, mouseY) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new BlockSelectScreen(setting, mc.screen));
		return true;
	}
}
