package com.hackclient.gui;

import com.hackclient.gui.component.ModuleButton;
import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.module.ModuleManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** A draggable, collapsible column of modules for one category. */
public class CategoryPanel {
	private final Category category;
	private final List<ModuleButton> buttons = new ArrayList<>();
	private int x, y;
	private int width = Theme.PANEL_MIN_WIDTH;

	private boolean expanded = true;
	private boolean dragging;
	private int dragOffsetX, dragOffsetY;

	public CategoryPanel(Category category, int x, int y, ModuleManager manager) {
		this.category = category;
		this.x = x;
		this.y = y;
		for (Module module : manager.getModules()) {
			if (module.getCategory() == category) buttons.add(new ModuleButton(module));
		}
	}

	public boolean isEmpty() {
		return buttons.isEmpty();
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/** Pulls the panel back inside the screen so its header can always be grabbed. */
	public void keepOnScreen(int screenWidth, int screenHeight) {
		x = Math.clamp(x, 0, Math.max(0, screenWidth - width));
		y = Math.clamp(y, 0, Math.max(0, screenHeight - Theme.PANEL_HEADER));
	}

	public Category getCategory() {
		return category;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public boolean isExpanded() {
		return expanded;
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	/** Width that fits the title and the longest module name (plus room for the settings dots). */
	public int getContentWidth(Font font) {
		int widest = font.width(Text.bold(title())) + Theme.PAD * 2;
		for (ModuleButton button : buttons) widest = Math.max(widest, font.width(Text.of(button.getModule().getName())) + Theme.PAD * 2 + 10);
		return Math.max(Theme.PANEL_MIN_WIDTH, widest);
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return Theme.PANEL_HEADER + (expanded ? buttons.size() * Theme.MODULE_ROW + 4 : 0);
	}

	/** @return the module under the mouse, for the description line */
	public Module render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		int bodyHeight = expanded ? buttons.size() * Theme.MODULE_ROW + 4 : 0;

		// Outline around the whole panel
		graphics.fill(x - 1, y - 1, x + width + 1, y + Theme.PANEL_HEADER + bodyHeight + 1, Theme.OUTLINE);

		graphics.fill(x, y, x + width, y + Theme.PANEL_HEADER, Theme.HEADER);
		graphics.fill(x, y + Theme.PANEL_HEADER - 2, x + width, y + Theme.PANEL_HEADER, Theme.ACCENT);
		Component title = Text.bold(title());
		graphics.drawString(font, Text.of(title), x + (width - font.width(Text.of(title))) / 2, Theme.textY(y, Theme.PANEL_HEADER - 2), Theme.TEXT, true);

		if (!expanded) return null;

		graphics.fill(x, y + Theme.PANEL_HEADER, x + width, y + Theme.PANEL_HEADER + bodyHeight, Theme.BACKGROUND);

		Module hovered = null;
		int buttonY = y + Theme.PANEL_HEADER + 2;
		for (ModuleButton button : buttons) {
			button.setBounds(x, buttonY, width);
			button.render(graphics, font, mouseX, mouseY);
			if (button.isHovered(mouseX, mouseY)) hovered = button.getModule();
			buttonY += button.getHeight();
		}
		return hovered;
	}

	public boolean isOverHeader(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + Theme.PANEL_HEADER;
	}

	/** @return the module row under the mouse, or null */
	public Module moduleAt(double mouseX, double mouseY) {
		if (!expanded) return null;
		for (ModuleButton button : buttons) {
			if (button.isHovered(mouseX, mouseY)) return button.getModule();
		}
		return null;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isOverHeader(mouseX, mouseY)) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				dragging = true;
				dragOffsetX = (int) mouseX - x;
				dragOffsetY = (int) mouseY - y;
			} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				expanded = !expanded;
			}
			return true;
		}
		if (!expanded) return false;

		for (ModuleButton moduleButton : buttons) {
			if (moduleButton.mouseClicked(mouseX, mouseY, button)) return true;
		}
		return false;
	}

	public void mouseReleased() {
		dragging = false;
	}

	public void mouseDragged(double mouseX, double mouseY) {
		if (dragging) {
			x = (int) mouseX - dragOffsetX;
			y = (int) mouseY - dragOffsetY;
		}
	}

	private String title() {
		String name = category.name();
		return name.charAt(0) + name.substring(1).toLowerCase();
	}
}
