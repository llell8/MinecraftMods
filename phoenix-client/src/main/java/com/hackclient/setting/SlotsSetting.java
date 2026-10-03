package com.hackclient.setting;

/** A set of hotbar slots (1-9), stored as a bitmask. Bit 0 = slot 1. */
public class SlotsSetting extends Setting<Integer> {
	public static final int ALL = 0b1_1111_1111;

	public SlotsSetting(String name, int defaultMask) {
		super(name, defaultMask);
	}

	/** @param index hotbar index 0-8 */
	public boolean isEnabled(int index) {
		return (value & (1 << index)) != 0;
	}

	public void toggle(int index) {
		value ^= 1 << index;
	}

	@Override
	public void set(Integer value) {
		this.value = value & ALL;
	}
}
