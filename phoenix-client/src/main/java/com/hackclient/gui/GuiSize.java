package com.hackclient.gui;

/**
 * Sizes for the click GUI, as real screen pixels per GUI pixel.
 * Fixed steps (instead of a free slider) so every option looks different and text stays sharp.
 */
public enum GuiSize {
	TINY("Tiny", 1.5),
	NORMAL("Normal", 2.0),
	MEDIUM("Medium", 2.5),
	LARGE("Large", 3.0),
	HUGE("Huge", 3.5),
	GIANT("Giant", 4.0);

	private final String display;
	final double pixels;

	GuiSize(String display, double pixels) {
		this.display = display;
		this.pixels = pixels;
	}

	@Override
	public String toString() {
		String multiplier = pixels == Math.floor(pixels) ? String.valueOf((int) pixels) : String.valueOf(pixels);
		return display + " (" + multiplier + "x)";
	}
}
