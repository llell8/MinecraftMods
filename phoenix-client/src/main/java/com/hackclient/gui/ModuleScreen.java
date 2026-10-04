package com.hackclient.gui;

import com.hackclient.gui.component.BlockListComponent;
import com.hackclient.gui.component.ColorComponent;
import com.hackclient.setting.ColorSetting;
import com.hackclient.setting.BlockListSetting;

import com.hackclient.gui.component.ActiveComponent;
import com.hackclient.gui.component.BindComponent;
import com.hackclient.gui.component.BoolComponent;
import com.hackclient.gui.component.Component;
import com.hackclient.gui.component.GroupComponent;
import com.hackclient.gui.component.ModeComponent;
import com.hackclient.gui.component.MultiComponent;
import com.hackclient.gui.component.ResetComponent;
import com.hackclient.gui.component.ResettableComponent;
import com.hackclient.gui.component.SliderComponent;
import com.hackclient.gui.component.SlotsComponent;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.ModeSetting;
import com.hackclient.setting.MultiSetting;
import com.hackclient.setting.NumberSetting;
import com.hackclient.setting.Setting;
import com.hackclient.setting.SettingGroup;
import com.hackclient.setting.SlotsSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Meteor-style settings window for one module. Scroll to see more; Esc goes back to the click GUI. */
public class ModuleScreen extends ScaledScreen {
	private static final int WIDTH = 210;
	private static final int TITLE_HEIGHT = 18;
	private static final int DESCRIPTION_HEIGHT = 13;
	private static final int MARGIN = 16;
	private static final int SCROLL_SPEED = 15;

	private final Module module;
	private final Screen parent;
	private final List<Component> rows = new ArrayList<>();
	private double scroll;

	public ModuleScreen(Module module, Screen parent) {
		super(module.getName());
		this.module = module;
		this.parent = parent;

		rows.add(new ActiveComponent(module));

		// Settings in a group go inside that group's collapsible section, in declaration order
		Map<SettingGroup, GroupComponent> groups = new HashMap<>();
		for (Setting<?> setting : module.getSettings()) {
			Component component = componentFor(setting);
			if (component == null) continue;

			SettingGroup group = setting.getGroup();
			if (group == null) {
				rows.add(component);
				continue;
			}
			groups.computeIfAbsent(group, g -> {
				GroupComponent created = new GroupComponent(g);
				rows.add(created);
				return created;
			}).add(component);
		}

		rows.add(new BindComponent(module));
		if (!module.getSettings().isEmpty()) rows.add(new ResetComponent(module));
	}

	private static Component componentFor(Setting<?> setting) {
		Component component;
		if (setting instanceof BoolSetting bool) component = new BoolComponent(bool);
		else if (setting instanceof NumberSetting number) component = new SliderComponent(number);
		else if (setting instanceof SlotsSetting slots) component = new SlotsComponent(slots);
		else if (setting instanceof ModeSetting<?> mode) component = new ModeComponent(mode);
		else if (setting instanceof MultiSetting<?> multi) component = new MultiComponent(multi);
		else if (setting instanceof BlockListSetting blocks) component = new BlockListComponent(blocks);
		else if (setting instanceof ColorSetting color) component = new ColorComponent(color);
		else return null;
		return new ResettableComponent(component, setting);
	}

	// ---- Layout ----

	private int windowX() {
		return (scaledWidth() - WIDTH) / 2;
	}

	private int windowY() {
		return Math.max(MARGIN, (scaledHeight() - windowHeight()) / 2);
	}

	private int contentTop() {
		return windowY() + TITLE_HEIGHT + DESCRIPTION_HEIGHT;
	}

	private int contentHeight() {
		return rows.stream().mapToInt(Component::getHeight).sum();
	}

	/** Visible height of the scrolling area. */
	private int viewHeight() {
		int max = scaledHeight() - MARGIN * 2 - TITLE_HEIGHT - DESCRIPTION_HEIGHT;
		return Math.min(contentHeight(), max);
	}

	private int windowHeight() {
		return TITLE_HEIGHT + DESCRIPTION_HEIGHT + viewHeight();
	}

	private void clampScroll() {
		scroll = Math.clamp(scroll, 0, Math.max(0, contentHeight() - viewHeight()));
	}

	// ---- Rendering ----

	@Override
	protected void renderScaled(GuiGraphics graphics, int mouseX, int mouseY) {
		clampScroll();
		int x = windowX();
		int y = windowY();
		int bottom = y + windowHeight();

		// Window frame
		graphics.fill(x - 1, y - 1, x + WIDTH + 1, bottom + 1, Theme.OUTLINE);
		graphics.fill(x, y, x + WIDTH, y + TITLE_HEIGHT, Theme.HEADER);
		graphics.fill(x, y + TITLE_HEIGHT - 2, x + WIDTH, y + TITLE_HEIGHT, Theme.ACCENT);

		net.minecraft.network.chat.Component title = Text.bold(module.getName());
		graphics.drawString(font, Text.of(title), x + Theme.PAD, Theme.textY(y, TITLE_HEIGHT - 2), Theme.TEXT, true);
		String category = module.getCategory().name().charAt(0) + module.getCategory().name().substring(1).toLowerCase();
		graphics.drawString(font, Text.of(category), x + WIDTH - font.width(Text.of(category)) - Theme.PAD, Theme.textY(y, TITLE_HEIGHT - 2), Theme.TEXT_DIM, true);

		graphics.fill(x, y + TITLE_HEIGHT, x + WIDTH, y + TITLE_HEIGHT + DESCRIPTION_HEIGHT, Theme.BACKGROUND);
		graphics.drawString(font, Text.of(Text.trim(font, module.getDescription(), WIDTH - Theme.PAD * 2)), x + Theme.PAD, Theme.textY(y + TITLE_HEIGHT, DESCRIPTION_HEIGHT), Theme.TEXT_DIM, true);

		// Scrolling rows, clipped to the window
		int top = contentTop();
		graphics.enableScissor(x, top, x + WIDTH, top + viewHeight());
		int rowY = top - (int) scroll;
		boolean mouseInView = mouseY >= top && mouseY < top + viewHeight();
		for (Component row : rows) {
			row.setBounds(x, rowY, WIDTH);
			row.render(graphics, font, mouseInView ? mouseX : -1, mouseInView ? mouseY : -1);
			rowY += row.getHeight();
		}
		graphics.disableScissor();

		// Scrollbar
		if (contentHeight() > viewHeight()) {
			int view = viewHeight();
			int barHeight = Math.max(12, view * view / contentHeight());
			int barY = top + (int) ((view - barHeight) * (scroll / (contentHeight() - view)));
			graphics.fill(x + WIDTH - 2, barY, x + WIDTH, barY + barHeight, Theme.ACCENT);
		}

		graphics.drawCenteredString(font, Text.of("Esc to go back   |   scroll for more"), scaledWidth() / 2, scaledHeight() - 16, Theme.TEXT_DIM);
	}

	// ---- Input ----

	@Override
	protected boolean onClick(double mouseX, double mouseY, int button) {
		int top = contentTop();
		boolean inView = mouseX >= windowX() && mouseX < windowX() + WIDTH && mouseY >= top && mouseY < top + viewHeight();

		// Every row sees the click (so e.g. the bind row can stop listening), but only rows in view can be hit
		boolean handled = false;
		for (Component row : rows) {
			if (row.mouseClicked(inView ? mouseX : -1, inView ? mouseY : -1, button)) handled = true;
		}

		// Right-clicking outside the window goes back
		boolean inWindow = mouseX >= windowX() && mouseX < windowX() + WIDTH && mouseY >= windowY() && mouseY < windowY() + windowHeight();
		if (!inWindow && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			onClose();
			return true;
		}
		return handled;
	}

	@Override
	protected void onRelease() {
		rows.forEach(Component::mouseReleased);
	}

	@Override
	protected void onDrag(double mouseX, double mouseY) {
		rows.forEach(row -> row.mouseDragged(mouseX, mouseY));
	}

	@Override
	protected boolean onScroll(double mouseX, double mouseY, double amount) {
		scroll -= amount * SCROLL_SPEED;
		clampScroll();
		return true;
	}

	@Override
	protected boolean onKey(int key) {
		for (Component row : rows) {
			if (row.keyPressed(key)) return true;
		}
		return false;
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
