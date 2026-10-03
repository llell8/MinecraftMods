package com.hackclient.setting;

/** A collapsible section of settings in the click GUI. */
public class SettingGroup {
	private final String name;
	private final boolean openByDefault;

	public SettingGroup(String name, boolean openByDefault) {
		this.name = name;
		this.openByDefault = openByDefault;
	}

	public String getName() {
		return name;
	}

	public boolean isOpenByDefault() {
		return openByDefault;
	}
}
