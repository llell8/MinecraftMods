package com.hackclient.gui;

import com.hackclient.setting.HasIcon;
import com.hackclient.setting.MultiSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Meteor-style item picker: "Selected" on the left, "Available" on the right.
 * Click an entry to move it to the other side. Esc goes back.
 */
public class MultiSelectScreen extends ScaledScreen {
	private static final int COLUMN_WIDTH = 120;
	private static final int GAP = 6;
	private static final int WIDTH = COLUMN_WIDTH * 2 + GAP * 3;
	private static final int TITLE_HEIGHT = 18;
	private static final int BUTTON_ROW = 18;
	private static final int COLUMN_HEADER = 13;
	private static final int ENTRY = 18;

	private final MultiSetting<?> setting;
	private final Screen parent;

	public MultiSelectScreen(MultiSetting<?> setting, Screen parent) {
		super(setting.getName());
		this.setting = setting;
		this.parent = parent;
	}

	private int windowX() {
		return (scaledWidth() - WIDTH) / 2;
	}

	private int windowY() {
		return Math.max(16, (scaledHeight() - windowHeight()) / 2);
	}

	private int windowHeight() {
		return TITLE_HEIGHT + BUTTON_ROW + COLUMN_HEADER + setting.getValues().length * ENTRY + GAP;
	}

	private int listTop() {
		return windowY() + TITLE_HEIGHT + BUTTON_ROW + COLUMN_HEADER;
	}

	private int columnX(boolean selectedColumn) {
		return windowX() + GAP + (selectedColumn ? 0 : COLUMN_WIDTH + GAP);
	}

	@Override
	protected void renderScaled(GuiGraphics graphics, int mouseX, int mouseY) {
		int x = windowX();
		int y = windowY();

		// Frame and title
		graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + windowHeight() + 1, Theme.OUTLINE);
		graphics.fill(x, y, x + WIDTH, y + windowHeight(), Theme.BACKGROUND);
		graphics.fill(x, y, x + WIDTH, y + TITLE_HEIGHT, Theme.HEADER);
		graphics.fill(x, y + TITLE_HEIGHT - 2, x + WIDTH, y + TITLE_HEIGHT, Theme.ACCENT);
		Component title = Text.bold(setting.getName());
		graphics.drawString(font, Text.of(title), x + Theme.PAD, Theme.textY(y, TITLE_HEIGHT - 2), Theme.TEXT, true);

		// Select all / Clear buttons
		int buttonY = y + TITLE_HEIGHT + 3;
		drawButton(graphics, "Select all", allButtonX(), buttonY, mouseX, mouseY);
		drawButton(graphics, "Clear", clearButtonX(), buttonY, mouseX, mouseY);

		// Columns
		int headerY = y + TITLE_HEIGHT + BUTTON_ROW;
		drawColumn(graphics, true, headerY, mouseX, mouseY);
		drawColumn(graphics, false, headerY, mouseX, mouseY);

		graphics.drawCenteredString(font, Text.of("Click an item to move it   |   Esc to go back"), scaledWidth() / 2, scaledHeight() - 16, Theme.TEXT_DIM);
	}

	private void drawColumn(GuiGraphics graphics, boolean selectedColumn, int headerY, int mouseX, int mouseY) {
		int cx = columnX(selectedColumn);
		List<Enum<?>> entries = entries(selectedColumn);

		String header = (selectedColumn ? "Selected" : "Available") + " (" + entries.size() + ")";
		graphics.drawString(font, Text.of(header), cx + 2, headerY + 3, selectedColumn ? Theme.ACCENT : Theme.TEXT_DIM, true);

		int top = listTop();
		graphics.fill(cx, top, cx + COLUMN_WIDTH, top + setting.getValues().length * ENTRY, Theme.SETTING_BACKGROUND);

		int entryY = top;
		for (Enum<?> value : entries) {
			boolean hovered = mouseX >= cx && mouseX < cx + COLUMN_WIDTH && mouseY >= entryY && mouseY < entryY + ENTRY;
			if (selectedColumn) graphics.fill(cx, entryY, cx + 2, entryY + ENTRY, Theme.ACCENT);
			if (hovered) graphics.fill(cx, entryY, cx + COLUMN_WIDTH, entryY + ENTRY, Theme.HOVER);

			graphics.renderItem(((HasIcon) value).icon(), cx + 3, entryY + 1);
			graphics.drawString(font, Text.of(value.toString()), cx + 23, Theme.textY(entryY, ENTRY), selectedColumn ? Theme.TEXT : Theme.TEXT_DISABLED, true);

			String arrow = selectedColumn ? ">" : "<";
			if (hovered) graphics.drawString(font, Text.of(arrow), cx + COLUMN_WIDTH - 10, Theme.textY(entryY, ENTRY), Theme.ACCENT, true);
			entryY += ENTRY;
		}
	}

	private void drawButton(GuiGraphics graphics, String text, int bx, int by, int mouseX, int mouseY) {
		int bw = buttonWidth(text);
		boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 12;
		graphics.fill(bx, by, bx + bw, by + 12, hovered ? Theme.ACCENT : Theme.OUTLINE);
		graphics.drawString(font, Text.of(text), bx + 5, by + 3, Theme.TEXT, true);
	}

	private int buttonWidth(String text) {
		return font.width(Text.of(text)) + 10;
	}

	private int allButtonX() {
		return windowX() + GAP;
	}

	private int clearButtonX() {
		return allButtonX() + buttonWidth("Select all") + 6;
	}

	private List<Enum<?>> entries(boolean selectedColumn) {
		List<Enum<?>> list = new ArrayList<>();
		for (Enum<?> value : setting.getValues()) {
			if (isEnabled(value) == selectedColumn) list.add(value);
		}
		return list;
	}

	@Override
	protected boolean onClick(double mouseX, double mouseY, int button) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) onClose();
			return true;
		}

		int buttonY = windowY() + TITLE_HEIGHT + 3;
		if (mouseY >= buttonY && mouseY < buttonY + 12) {
			if (mouseX >= allButtonX() && mouseX < allButtonX() + buttonWidth("Select all")) {
				setting.setAll(true);
				return true;
			}
			if (mouseX >= clearButtonX() && mouseX < clearButtonX() + buttonWidth("Clear")) {
				setting.setAll(false);
				return true;
			}
		}

		for (boolean selectedColumn : new boolean[] {true, false}) {
			int cx = columnX(selectedColumn);
			if (mouseX < cx || mouseX >= cx + COLUMN_WIDTH) continue;
			int index = (int) ((mouseY - listTop()) / ENTRY);
			List<Enum<?>> entries = entries(selectedColumn);
			if (mouseY >= listTop() && index >= 0 && index < entries.size()) {
				toggle(entries.get(index));
				return true;
			}
		}
		return false;
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
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
