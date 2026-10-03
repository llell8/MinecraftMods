package com.hackclient.setting;

/** One choice out of an enum. Enum constants should override toString() with a display name. */
public class ModeSetting<E extends Enum<E>> extends Setting<E> {
	private final E[] values;

	public ModeSetting(String name, E defaultValue) {
		super(name, defaultValue);
		this.values = defaultValue.getDeclaringClass().getEnumConstants();
	}

	public void cycle(boolean forward) {
		int next = (value.ordinal() + (forward ? 1 : values.length - 1)) % values.length;
		value = values[next];
	}

	/** Sets the value from its enum constant name; ignores unknown names (e.g. from an old config). */
	public void setByName(String name) {
		for (E e : values) {
			if (e.name().equals(name)) value = e;
		}
	}
}
