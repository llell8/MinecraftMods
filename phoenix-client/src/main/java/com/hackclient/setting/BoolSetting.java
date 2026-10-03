package com.hackclient.setting;

public class BoolSetting extends Setting<Boolean> {
	public BoolSetting(String name, boolean defaultValue) {
		super(name, defaultValue);
	}

	public void toggle() {
		value = !value;
	}
}
