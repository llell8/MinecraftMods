package com.hackclient.module.modules.misc;

import com.hackclient.gui.ClickGuiScreen;
import com.hackclient.gui.FontChoice;
import com.hackclient.gui.GuiSize;
import com.hackclient.gui.Styles;
import com.hackclient.gui.ThemePreset;
import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.ModeSetting;
import com.hackclient.setting.NumberSetting;
import com.hackclient.setting.SettingGroup;
import org.lwjgl.glfw.GLFW;

/** Opens the click GUI (never stays enabled). Also holds the GUI's look: theme and scale. */
public class ClickGui extends Module {
	private final SettingGroup sgAppearance = group("Appearance");
	private final ModeSetting<ThemePreset> theme = mode("Theme", ThemePreset.PHOENIX);
	private final NumberSetting customHue = number("Custom hue", 270, 0, 360, 0);
	private final ModeSetting<FontChoice> font = mode("Font", FontChoice.MINECRAFT);
	private final ModeSetting<GuiSize> size = mode("GUI size", GuiSize.NORMAL);

	private final SettingGroup sgButtons = group("Buttons");
	private final ModeSetting<Styles.Checkbox> checkboxStyle = mode("Checkbox style", Styles.Checkbox.BOX);
	private final ModeSetting<Styles.Slider> sliderStyle = mode("Slider style", Styles.Slider.LINE);
	private final ModeSetting<Styles.ModuleRow> moduleStyle = mode("Module style", Styles.ModuleRow.BAR);
	private final ModeSetting<Styles.SettingsIcon> settingsIcon = mode("Settings icon", Styles.SettingsIcon.DOTS);

	public ClickGui() {
		super("ClickGUI", "Opens this menu. Change the theme and size here.", Category.MISC, GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	public ThemePreset getTheme() {
		return theme.get();
	}

	public FontChoice getFont() {
		return font.get();
	}

	public float getCustomHue() {
		return customHue.getFloat();
	}

	public GuiSize getSize() {
		return size.get();
	}

	public Styles.Checkbox getCheckboxStyle() {
		return checkboxStyle.get();
	}

	public Styles.Slider getSliderStyle() {
		return sliderStyle.get();
	}

	public Styles.ModuleRow getModuleStyle() {
		return moduleStyle.get();
	}

	public Styles.SettingsIcon getSettingsIcon() {
		return settingsIcon.get();
	}

	@Override
	protected void onEnable() {
		if (mc.player != null) mc.setScreen(new ClickGuiScreen());
		setEnabled(false);
	}
}
