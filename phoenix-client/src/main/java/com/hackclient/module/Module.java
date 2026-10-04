package com.hackclient.module;

import com.hackclient.setting.BlockListSetting;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.ModeSetting;
import com.hackclient.setting.MultiSetting;
import com.hackclient.setting.NumberSetting;
import com.hackclient.setting.Setting;
import com.hackclient.setting.SettingGroup;
import com.hackclient.setting.SlotsSetting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
	protected static final Minecraft mc = Minecraft.getInstance();

	private final String name;
	private final String description;
	private final Category category;
	private final List<Setting<?>> settings = new ArrayList<>();
	private SettingGroup currentGroup;
	private int key;
	private boolean enabled;

	/**
	 * @param key a GLFW key code (e.g. GLFW.GLFW_KEY_R), or GLFW.GLFW_KEY_UNKNOWN for no bind
	 */
	protected Module(String name, String description, Category category, int key) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.key = key;
	}

	/**
	 * Starts a collapsible section: every setting declared after this (until the next group) goes in it.
	 * Use as a field, e.g. {@code private final SettingGroup sgTiming = group("Timing");}
	 */
	protected SettingGroup group(String name) {
		return group(name, true);
	}

	protected SettingGroup group(String name, boolean openByDefault) {
		currentGroup = new SettingGroup(name, openByDefault);
		return currentGroup;
	}

	protected BoolSetting bool(String name, boolean defaultValue) {
		return add(new BoolSetting(name, defaultValue));
	}

	protected NumberSetting number(String name, double defaultValue, double min, double max, int decimals) {
		return add(new NumberSetting(name, defaultValue, min, max, decimals));
	}

	protected <E extends Enum<E>> ModeSetting<E> mode(String name, E defaultValue) {
		return add(new ModeSetting<>(name, defaultValue));
	}

	protected <E extends Enum<E>> MultiSetting<E> multi(String name, Class<E> type, int defaultMask) {
		return add(new MultiSetting<>(name, type, defaultMask));
	}

	protected BlockListSetting blocks(String name, String... defaultIds) {
		return add(new BlockListSetting(name, java.util.List.of(defaultIds)));
	}

	protected SlotsSetting slots(String name, int defaultMask) {
		return add(new SlotsSetting(name, defaultMask));
	}

	private <S extends Setting<?>> S add(S setting) {
		setting.setGroup(currentGroup);
		settings.add(setting);
		return setting;
	}

	/** Puts every setting back to its default value. Keeps the keybind and on/off state. */
	public void resetSettings() {
		settings.forEach(Setting::reset);
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		if (enabled) onEnable();
		else onDisable();
	}

	/** Note: may be called before a world is loaded (e.g. when loading the config), so mc.player can be null. */
	protected void onEnable() {}

	/** Note: mc.player can be null here. */
	protected void onDisable() {}

	/** Called every client tick while enabled and in a world. */
	public void onTick() {}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public List<Setting<?>> getSettings() {
		return settings;
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}

	public boolean isEnabled() {
		return enabled;
	}
}
