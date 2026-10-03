package com.hackclient.gui.component;

import com.hackclient.gui.Pixels;
import com.hackclient.gui.Text;
import com.hackclient.gui.Theme;
import com.hackclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** One row, Meteor-style: name on the left, then the bar, then the value. */
public class SliderComponent extends Component {
	private static final int TRACK_HEIGHT = 2;
	private final NumberSetting setting;
	private boolean dragging;

	public SliderComponent(NumberSetting setting) {
		this.setting = setting;
	}

	@Override
	public int getHeight() {
		return Theme.ROW;
	}

	@Override
	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		graphics.fill(x, y, x + width, y + Theme.ROW, Theme.SETTING_BACKGROUND);
		if (isHovered(mouseX, mouseY) || dragging) graphics.fill(x, y, x + width, y + Theme.ROW, Theme.HOVER);

		int textY = Theme.textY(y, Theme.ROW);
		graphics.drawString(font, Text.of(setting.getName()), x + Theme.PAD, textY, Theme.TEXT_DISABLED, true);
		String value = setting.format();
		graphics.drawString(font, Text.of(value), x + width - font.width(Text.of(value)) - Theme.PAD, textY, Theme.TEXT, true);

		int left = trackLeft(font);
		int right = trackRight(font);
		double progress = (setting.get() - setting.getMin()) / (setting.getMax() - setting.getMin());
		int filled = left + (int) ((right - left) * progress);
		int centerY = y + Theme.ROW / 2;

		switch (Theme.sliderStyle) {
			case LINE -> {
				int trackY = centerY - 1;
				graphics.fill(left, trackY, right, trackY + TRACK_HEIGHT, Theme.OUTLINE);
				graphics.fill(left, trackY, filled, trackY + TRACK_HEIGHT, Theme.ACCENT);
				graphics.fill(filled - 1, trackY - 2, filled + 2, trackY + TRACK_HEIGHT + 2, Theme.TEXT);
			}
			case BAR -> {
				Pixels.roundedRect(graphics, left, centerY - 4, right, centerY + 4, Theme.OUTLINE);
				if (filled > left + 1) Pixels.roundedRect(graphics, left, centerY - 4, filled, centerY + 4, Theme.ACCENT);
			}
			case DOT -> {
				graphics.fill(left, centerY, right, centerY + 1, Theme.OUTLINE);
				graphics.fill(left, centerY, filled, centerY + 1, Theme.ACCENT);
				Pixels.draw(graphics, Pixels.DOT, filled - 2, centerY - 2, Theme.TEXT);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isHovered(mouseX, mouseY) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
		dragging = true;
		update(mouseX);
		return true;
	}

	@Override
	public void mouseDragged(double mouseX, double mouseY) {
		if (dragging) update(mouseX);
	}

	@Override
	public void mouseReleased() {
		dragging = false;
	}

	private void update(double mouseX) {
		Font font = Minecraft.getInstance().font;
		double progress = Math.clamp((mouseX - trackLeft(font)) / (trackRight(font) - trackLeft(font)), 0.0, 1.0);
		setting.set(setting.getMin() + progress * (setting.getMax() - setting.getMin()));
	}

	/** Bar starts after the name (but at least at 45% of the row) so long names never overlap it. */
	private int trackLeft(Font font) {
		return x + Math.max(font.width(Text.of(setting.getName())) + Theme.PAD * 2, (int) (width * 0.45));
	}

	/** Bar ends before the widest the value text can get. */
	private int trackRight(Font font) {
		int valueWidth = font.width(Text.of(setting.format()));
		return x + width - Theme.PAD - Math.max(valueWidth, 18) - 6;
	}
}
