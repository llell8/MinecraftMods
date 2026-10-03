package com.hackclient.gui;

/** Fonts bundled in assets/hackclient/font (all SIL Open Font License, see resources/font-licenses). */
public enum FontChoice {
	MINECRAFT("Minecraft", null),
	INTER("Inter", "inter"),
	POPPINS("Poppins", "poppins"),
	NUNITO("Nunito", "nunito"),
	COMFORTAA("Comfortaa", "comfortaa"),
	JETBRAINS_MONO("JetBrains Mono", "jetbrains_mono");

	private final String display;
	final String id;

	FontChoice(String display, String id) {
		this.display = display;
		this.id = id;
	}

	@Override
	public String toString() {
		return display;
	}
}
