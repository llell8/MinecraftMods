package com.hackclient.gui;

import com.hackclient.HackClient;
import com.hackclient.module.Category;
import com.hackclient.module.Module;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Click GUI: one draggable panel per category.
 * Left-click a module to toggle it, right-click to open its settings window.
 */
public class ClickGuiScreen extends ScaledScreen {
	private static final int GAP = 6;

	// Panels live as long as the game; their positions are also saved to the config (see GuiLayout)
	private static final List<CategoryPanel> PANELS = new ArrayList<>();

	public ClickGuiScreen() {
		super("ClickGUI");
	}

	@Override
	protected void init() {
		Theme.update(); // make sure the current font is applied before measuring text
		Text.setPixelScale(pixelScale());

		boolean firstOpen = PANELS.isEmpty();
		if (firstOpen) {
			for (Category category : Category.values()) {
				CategoryPanel panel = new CategoryPanel(category, 0, 0, HackClient.getModuleManager());
				if (!panel.isEmpty()) PANELS.add(panel);
			}
		}

		// All panels share the width of the widest one (redone every time, the font may have changed)
		int panelWidth = 0;
		for (CategoryPanel panel : PANELS) panelWidth = Math.max(panelWidth, panel.getContentWidth(font));
		for (CategoryPanel panel : PANELS) panel.setWidth(panelWidth);

		if (firstOpen) placePanels(panelWidth);

		// Keep panels reachable if the window or GUI size changed since they were placed
		for (CategoryPanel panel : PANELS) panel.keepOnScreen(scaledWidth(), scaledHeight());
	}

	/** Restores saved positions; panels without one go in a row from the top-left, wrapping when the screen is full. */
	private void placePanels(int panelWidth) {
		int x = GAP;
		int y = GAP;
		int rowHeight = 0;
		for (CategoryPanel panel : PANELS) {
			GuiLayout.PanelState saved = GuiLayout.get(panel.getCategory().name());
			if (saved != null) {
				panel.setExpanded(saved.expanded());
				panel.setPosition(saved.x(), saved.y());
				continue;
			}
			if (x + panelWidth > scaledWidth() && x > GAP) {
				x = GAP;
				y += rowHeight + GAP;
				rowHeight = 0;
			}
			panel.setPosition(x, y);
			x += panelWidth + GAP;
			rowHeight = Math.max(rowHeight, panel.getHeight());
		}
	}

	@Override
	public void removed() {
		for (CategoryPanel panel : PANELS) {
			GuiLayout.put(panel.getCategory().name(), new GuiLayout.PanelState(panel.getX(), panel.getY(), panel.isExpanded()));
		}
		super.removed(); // saves the config
	}

	@Override
	protected void renderScaled(GuiGraphics graphics, int mouseX, int mouseY) {
		Module hovered = null;
		for (CategoryPanel panel : PANELS) {
			Module module = panel.render(graphics, font, mouseX, mouseY);
			if (module != null) hovered = module;
		}

		String hint = hovered != null
				? hovered.getDescription()
				: "Left-click: toggle   |   Right-click: settings   |   Drag headers to move";
		graphics.drawCenteredString(font, Text.of(hint), scaledWidth() / 2, scaledHeight() - 12, hovered != null ? Theme.TEXT : Theme.TEXT_DIM);
	}

	@Override
	protected boolean onClick(double mouseX, double mouseY, int button) {
		// Iterate in reverse so the top-most panel gets the click first
		for (int i = PANELS.size() - 1; i >= 0; i--) {
			CategoryPanel panel = PANELS.get(i);

			Module module = panel.moduleAt(mouseX, mouseY);
			if (module != null && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				minecraft.setScreen(new ModuleScreen(module, this));
				return true;
			}

			if (panel.mouseClicked(mouseX, mouseY, button)) {
				// Bring clicked panel to the front
				PANELS.remove(i);
				PANELS.add(panel);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onRelease() {
		PANELS.forEach(CategoryPanel::mouseReleased);
	}

	@Override
	protected void onDrag(double mouseX, double mouseY) {
		PANELS.forEach(panel -> panel.mouseDragged(mouseX, mouseY));
	}

}
