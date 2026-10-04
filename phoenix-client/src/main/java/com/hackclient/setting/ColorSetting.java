package com.hackclient.setting;

/** A colour with transparency, stored as ARGB (0xAARRGGBB). */
public class ColorSetting extends Setting<Integer> {
	public ColorSetting(String name, int defaultArgb) {
		super(name, defaultArgb);
	}

	/** The colour with its alpha replaced, e.g. for a box's translucent sides. */
	public int withAlpha(int alpha) {
		return (value & 0x00FFFFFF) | (Math.clamp(alpha, 0, 255) << 24);
	}
}
