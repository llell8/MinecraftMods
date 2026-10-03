package com.hackclient.setting;

public abstract class Setting<T> {
	private final String name;
	private final T defaultValue;
	protected T value;
	private SettingGroup group;

	protected Setting(String name, T defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String getName() {
		return name;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	public T getDefault() {
		return defaultValue;
	}

	public void reset() {
		set(defaultValue);
	}

	/** @return the GUI section this setting belongs to, or null if it isn't in one */
	public SettingGroup getGroup() {
		return group;
	}

	public void setGroup(SettingGroup group) {
		this.group = group;
	}
}
