package com.hackclient.setting;

/** A colour with transparency, stored as ARGB (0xAARRGGBB). */
public class ColorSetting extends Setting<Integer> {
	private Runnable onPicked;

	public ColorSetting(String name, int defaultArgb) {
		super(name, defaultArgb);
	}

	/** The colour with its alpha replaced, e.g. for a box's translucent sides. */
	public int withAlpha(int alpha) {
		return (value & 0x00FFFFFF) | (Math.clamp(alpha, 0, 255) << 24);
	}

	/**
	 * Runs when the user picks a colour in the GUI, e.g. to switch off "distance colours" so the picked
	 * colour actually shows. Returns this for chaining in field declarations.
	 */
	public ColorSetting onPicked(Runnable action) {
		this.onPicked = action;
		return this;
	}

	/** Called by the colour picker (not by config loading or reset). */
	public void pick(int argb) {
		set(argb);
		if (onPicked != null) onPicked.run();
	}
}
