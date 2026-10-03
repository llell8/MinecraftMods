package com.hackclient.gui;

import com.hackclient.HackClient;
import com.hackclient.module.modules.misc.ClickGui;
import net.minecraft.util.Mth;

/**
 * Colors (ARGB) and sizes shared by the GUI and HUD.
 * The themed colors change with ClickGUI's Theme setting; call {@link #update()} before drawing.
 */
public final class Theme {
	// Themed
	public static int ACCENT;
	public static int ACCENT_DARK;
	public static int ACCENT_FADED;
	public static int HEADER;
	public static int BACKGROUND;
	public static int SETTING_BACKGROUND;
	public static int SECTION;
	public static int OUTLINE;

	// Fixed
	public static final int HOVER = 0x18FFFFFF;
	public static final int TEXT = 0xFFFFFFFF;
	public static final int TEXT_DISABLED = 0xFFC8C8C8;
	public static final int TEXT_DIM = 0xFF8A8A8A;
	public static final int RED = 0xFFFF5555;

	// Widget designs (ClickGUI → Buttons)
	public static Styles.Checkbox checkboxStyle = Styles.Checkbox.BOX;
	public static Styles.Slider sliderStyle = Styles.Slider.LINE;
	public static Styles.ModuleRow moduleStyle = Styles.ModuleRow.BAR;
	public static Styles.SettingsIcon settingsIcon = Styles.SettingsIcon.DOTS;

	/** Height of a one-line setting row. */
	public static final int ROW = 13;
	/** Height of a module row in a category panel. */
	public static final int MODULE_ROW = 14;
	public static final int PANEL_MIN_WIDTH = 90;
	public static final int PANEL_HEADER = 17;
	/** Left padding for text inside rows. */
	public static final int PAD = 5;

	static {
		apply(ThemePreset.PHOENIX, 0);
	}

	private Theme() {}

	/** Pulls the current theme from the ClickGUI module's settings. Cheap; call once per frame. */
	public static void update() {
		ClickGui clickGui = HackClient.getModuleManager() != null ? HackClient.getModuleManager().get(ClickGui.class) : null;
		if (clickGui != null) {
			apply(clickGui.getTheme(), clickGui.getCustomHue());
			checkboxStyle = clickGui.getCheckboxStyle();
			sliderStyle = clickGui.getSliderStyle();
			moduleStyle = clickGui.getModuleStyle();
			settingsIcon = clickGui.getSettingsIcon();
		}
		Text.update();
	}

	public static void apply(ThemePreset preset, float hue) {
		if (preset == ThemePreset.CUSTOM) {
			float h = hue / 360f;
			ACCENT = hsb(h, 0.75f, 1.0f, 0xFF);
			ACCENT_DARK = hsb(h, 0.8f, 0.6f, 0xFF);
			HEADER = hsb(h, 0.35f, 0.13f, 0xF2);
			BACKGROUND = hsb(h, 0.3f, 0.08f, 0xE6);
			SETTING_BACKGROUND = hsb(h, 0.3f, 0.06f, 0xE6);
			SECTION = hsb(h, 0.3f, 0.11f, 0xF0);
			OUTLINE = hsb(h, 0.3f, 0.22f, 0xFF);
		} else {
			ACCENT = preset.accent;
			ACCENT_DARK = preset.accentDark;
			HEADER = preset.header;
			BACKGROUND = preset.background;
			SETTING_BACKGROUND = preset.settingBackground;
			SECTION = preset.section;
			OUTLINE = preset.outline;
		}
		ACCENT_FADED = (ACCENT & 0x00FFFFFF) | 0x55000000;
	}

	private static int hsb(float hue, float saturation, float brightness, int alpha) {
		return (alpha << 24) | (Mth.hsvToRgb(hue, saturation, brightness) & 0x00FFFFFF);
	}

	/** Y position that vertically centers a line of text in a row starting at y with the given height. */
	public static int textY(int y, int height) {
		return y + (height - 8) / 2 + 1;
	}
}
