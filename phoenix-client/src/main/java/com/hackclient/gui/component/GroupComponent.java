package com.hackclient.gui.component;

import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.setting.SettingGroup;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** A collapsible section header. Click it to show/hide the settings inside. */
public class GroupComponent extends com.hackclient.gui.component.Component {
	private static final int HEADER_HEIGHT = 14;

	private final SettingGroup group;
	private final List<com.hackclient.gui.component.Component> children = new ArrayList<>();
	private boolean open;

	public GroupComponent(SettingGroup group) {
		this.group = group;
		this.open = group.isOpenByDefault();
	}

	public void add(com.hackclient.gui.component.Component child) {
		children.add(child);
	}

	@Override
	public int getHeight() {
		if (!open) return HEADER_HEIGHT;
		int height = HEADER_HEIGHT;
		for (com.hackclient.gui.component.Component child : children) height += child.getHeight();
		return height;
	}

	@Override
	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		graphics.fill(x, y, x + width, y + HEADER_HEIGHT, Theme.SECTION);
		if (isHeaderHovered(mouseX, mouseY)) graphics.fill(x, y, x + width, y + HEADER_HEIGHT, Theme.HOVER);
		graphics.fill(x, y + HEADER_HEIGHT - 1, x + width, y + HEADER_HEIGHT, Theme.ACCENT_DARK);

		int textY = Theme.textY(y, HEADER_HEIGHT);
		graphics.drawString(font, Text.bold(group.getName()), x + Theme.PAD, textY, Theme.ACCENT, true);
		String arrow = open ? "-" : "+";
		graphics.drawString(font, Text.of(arrow), x + width - font.width(Text.of(arrow)) - Theme.PAD, textY, Theme.TEXT_DIM, true);

		if (!open) return;
		int childY = y + HEADER_HEIGHT;
		for (com.hackclient.gui.component.Component child : children) {
			child.setBounds(x, childY, width);
			child.render(graphics, font, mouseX, mouseY);
			childY += child.getHeight();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isHeaderHovered(mouseX, mouseY)) {
			open = !open;
			return true;
		}
		if (!open) return false;

		boolean handled = false;
		for (com.hackclient.gui.component.Component child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) handled = true;
		}
		return handled;
	}

	@Override
	public void mouseReleased() {
		children.forEach(com.hackclient.gui.component.Component::mouseReleased);
	}

	@Override
	public void mouseDragged(double mouseX, double mouseY) {
		if (open) children.forEach(child -> child.mouseDragged(mouseX, mouseY));
	}

	@Override
	public boolean keyPressed(int key) {
		if (!open) return false;
		for (com.hackclient.gui.component.Component child : children) {
			if (child.keyPressed(key)) return true;
		}
		return false;
	}

	private boolean isHeaderHovered(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEADER_HEIGHT;
	}
}
