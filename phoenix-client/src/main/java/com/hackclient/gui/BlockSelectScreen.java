package com.hackclient.gui;

import com.hackclient.setting.BlockListSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Block picker with every block in the game: "Selected" on the left, "Available" on the right.
 * Type to search, scroll each column, click an entry to move it across. Esc goes back.
 */
public class BlockSelectScreen extends ScaledScreen {
	private static final int COLUMN_WIDTH = 150;
	private static final int GAP = 6;
	private static final int WIDTH = COLUMN_WIDTH * 2 + GAP * 3;
	private static final int TITLE_HEIGHT = 18;
	private static final int SEARCH_ROW = 18;
	private static final int COLUMN_HEADER = 13;
	private static final int ENTRY = 18;

	private static List<Block> allBlocks;

	private final BlockListSetting setting;
	private final Screen parent;
	private String search = "";
	private final int[] scroll = new int[2];

	public BlockSelectScreen(BlockListSetting setting, Screen parent) {
		super(setting.getName());
		this.setting = setting;
		this.parent = parent;
		if (allBlocks == null) {
			allBlocks = new ArrayList<>();
			for (Block block : BuiltInRegistries.BLOCK) {
				if (block != Blocks.AIR && block != Blocks.CAVE_AIR && block != Blocks.VOID_AIR) allBlocks.add(block);
			}
			allBlocks.sort(Comparator.comparing(b -> b.getName().getString()));
		}
	}

	// ---- Layout ----

	private int windowX() {
		return (scaledWidth() - WIDTH) / 2;
	}

	private int visibleEntries() {
		return Math.max(3, (scaledHeight() - 60 - TITLE_HEIGHT - SEARCH_ROW - COLUMN_HEADER) / ENTRY);
	}

	private int windowHeight() {
		return TITLE_HEIGHT + SEARCH_ROW + COLUMN_HEADER + visibleEntries() * ENTRY + GAP;
	}

	private int windowY() {
		return Math.max(16, (scaledHeight() - windowHeight()) / 2);
	}

	private int listTop() {
		return windowY() + TITLE_HEIGHT + SEARCH_ROW + COLUMN_HEADER;
	}

	private int columnX(int column) {
		return windowX() + GAP + column * (COLUMN_WIDTH + GAP);
	}

	/** Column 0 = selected, 1 = available; both filtered by the search. */
	private List<Block> entries(int column) {
		String q = search.toLowerCase(Locale.ROOT);
		List<Block> list = new ArrayList<>();
		for (Block block : allBlocks) {
			boolean selected = setting.contains(block);
			if (selected != (column == 0)) continue;
			if (!q.isEmpty() && !block.getName().getString().toLowerCase(Locale.ROOT).contains(q)
					&& !BlockListSetting.id(block).contains(q.replace(' ', '_'))) continue;
			list.add(block);
		}
		return list;
	}

	// ---- Drawing ----

	@Override
	protected void renderScaled(GuiGraphics graphics, int mouseX, int mouseY) {
		int x = windowX();
		int y = windowY();

		graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + windowHeight() + 1, Theme.OUTLINE);
		graphics.fill(x, y, x + WIDTH, y + windowHeight(), Theme.BACKGROUND);
		graphics.fill(x, y, x + WIDTH, y + TITLE_HEIGHT, Theme.HEADER);
		graphics.fill(x, y + TITLE_HEIGHT - 2, x + WIDTH, y + TITLE_HEIGHT, Theme.ACCENT);
		graphics.drawString(font, Text.of(Text.bold(setting.getName())), x + Theme.PAD, Theme.textY(y, TITLE_HEIGHT - 2), Theme.TEXT, true);

		// Search box
		int sy = y + TITLE_HEIGHT + 3;
		graphics.fill(x + GAP, sy, x + WIDTH - GAP, sy + 12, Theme.SETTING_BACKGROUND);
		String shown = search.isEmpty() ? "Type to search..." : search + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
		graphics.drawString(font, Text.of(shown), x + GAP + 4, sy + 2, search.isEmpty() ? Theme.TEXT_DIM : Theme.TEXT, true);

		for (int column = 0; column < 2; column++) drawColumn(graphics, column, mouseX, mouseY);

		graphics.drawCenteredString(font, Text.of("Click to move  |  Scroll each list  |  Ctrl+A / Ctrl+D: all / none of the results  |  Esc: back"),
				scaledWidth() / 2, scaledHeight() - 16, Theme.TEXT_DIM);
	}

	private void drawColumn(GuiGraphics graphics, int column, int mouseX, int mouseY) {
		int cx = columnX(column);
		List<Block> entries = entries(column);
		scroll[column] = Math.max(0, Math.min(scroll[column], Math.max(0, entries.size() - visibleEntries())));

		String header = (column == 0 ? "Selected" : "Available") + " (" + entries.size() + ")";
		graphics.drawString(font, Text.of(header), cx + 2, listTop() - COLUMN_HEADER + 3, column == 0 ? Theme.ACCENT : Theme.TEXT_DIM, true);

		int top = listTop();
		int height = visibleEntries() * ENTRY;
		graphics.fill(cx, top, cx + COLUMN_WIDTH, top + height, Theme.SETTING_BACKGROUND);

		for (int i = 0; i < visibleEntries() && scroll[column] + i < entries.size(); i++) {
			Block block = entries.get(scroll[column] + i);
			int ey = top + i * ENTRY;
			boolean hovered = mouseX >= cx && mouseX < cx + COLUMN_WIDTH && mouseY >= ey && mouseY < ey + ENTRY;
			if (column == 0) graphics.fill(cx, ey, cx + 2, ey + ENTRY, Theme.ACCENT);
			if (hovered) graphics.fill(cx, ey, cx + COLUMN_WIDTH, ey + ENTRY, Theme.HOVER);
			ItemStack icon = new ItemStack(block.asItem());
			if (!icon.isEmpty()) graphics.renderItem(icon, cx + 3, ey + 1);
			String name = Text.trim(font, block.getName().getString(), COLUMN_WIDTH - 36);
			graphics.drawString(font, Text.of(name), cx + 23, Theme.textY(ey, ENTRY), column == 0 ? Theme.TEXT : Theme.TEXT_DISABLED, true);
			if (hovered) graphics.drawString(font, Text.of(column == 0 ? ">" : "<"), cx + COLUMN_WIDTH - 10, Theme.textY(ey, ENTRY), Theme.ACCENT, true);
		}

		// Scrollbar
		if (entries.size() > visibleEntries()) {
			int barHeight = Math.max(10, height * visibleEntries() / entries.size());
			int barY = top + (height - barHeight) * scroll[column] / (entries.size() - visibleEntries());
			graphics.fill(cx + COLUMN_WIDTH - 2, barY, cx + COLUMN_WIDTH, barY + barHeight, Theme.ACCENT);
		}
	}

	// ---- Input ----

	@Override
	protected boolean onClick(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			onClose();
			return true;
		}
		for (int column = 0; column < 2; column++) {
			int cx = columnX(column);
			if (mouseX < cx || mouseX >= cx + COLUMN_WIDTH || mouseY < listTop()) continue;
			int index = scroll[column] + (int) ((mouseY - listTop()) / ENTRY);
			List<Block> entries = entries(column);
			if ((mouseY - listTop()) / ENTRY < visibleEntries() && index < entries.size()) {
				setting.toggle(entries.get(index));
				return true;
			}
		}
		return false;
	}

	@Override
	protected boolean onScroll(double mouseX, double mouseY, double amount) {
		int column = mouseX >= columnX(1) ? 1 : 0;
		scroll[column] -= (int) Math.signum(amount) * 3;
		return true;
	}

	@Override
	protected boolean onKey(int key) {
		boolean ctrl = InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
				|| InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
		if (ctrl && key == GLFW.GLFW_KEY_A) {
			setting.setAll(entries(1), true);
			return true;
		}
		if (ctrl && key == GLFW.GLFW_KEY_D) {
			setting.setAll(entries(0), false);
			return true;
		}
		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
			resetScroll();
			return true;
		}
		char c = 0;
		if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) c = (char) ('a' + key - GLFW.GLFW_KEY_A);
		else if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) c = (char) ('0' + key - GLFW.GLFW_KEY_0);
		else if (key == GLFW.GLFW_KEY_SPACE) c = ' ';
		else if (key == GLFW.GLFW_KEY_MINUS) c = '_';
		if (c != 0 && search.length() < 30) {
			search += c;
			resetScroll();
			return true;
		}
		return false;
	}

	private void resetScroll() {
		scroll[0] = 0;
		scroll[1] = 0;
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
