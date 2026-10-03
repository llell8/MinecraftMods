package com.hackclient.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** A row in a panel. Position is assigned every frame by the parent before render. */
public abstract class Component {
	protected int x, y, width;

	public void setBounds(int x, int y, int width) {
		this.x = x;
		this.y = y;
		this.width = width;
	}

	public abstract int getHeight();

	public abstract void render(GuiGraphics graphics, Font font, int mouseX, int mouseY);

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	public void mouseReleased() {}

	public void mouseDragged(double mouseX, double mouseY) {}

	public boolean keyPressed(int key) {
		return false;
	}

	public boolean isHovered(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + getHeight();
	}
}
