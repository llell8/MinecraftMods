package com.hackclient.setting;

/**
 * Any number of choices out of an enum, stored as a bitmask (bit N = ordinal N).
 * Enum constants should override toString() with a display name.
 */
public class MultiSetting<E extends Enum<E>> extends Setting<Integer> {
	private final E[] values;

	public MultiSetting(String name, Class<E> type, int defaultMask) {
		super(name, defaultMask);
		this.values = type.getEnumConstants();
	}

	public static <E extends Enum<E>> int maskOf(Class<E> type) {
		return (1 << type.getEnumConstants().length) - 1;
	}

	public E[] getValues() {
		return values;
	}

	/** True if the options have item icons, so the GUI shows the Meteor-style picker. */
	public boolean hasIcons() {
		return values.length > 0 && values[0] instanceof HasIcon;
	}

	public void setAll(boolean enabled) {
		value = enabled ? (1 << values.length) - 1 : 0;
	}

	public boolean isEnabled(E value) {
		return (this.value & (1 << value.ordinal())) != 0;
	}

	public void toggle(E value) {
		this.value ^= 1 << value.ordinal();
	}

	@Override
	public void set(Integer value) {
		this.value = value & ((1 << values.length) - 1);
	}
}
