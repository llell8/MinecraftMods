package com.hackclient.gui;

/** Color templates for the GUI and HUD. CUSTOM builds its colors from the "Custom hue" slider instead. */
public enum ThemePreset {
	//            name         accent      dark        header      background  setting bg  section     outline
	PHOENIX("Phoenix",   0xFFFF7A1A, 0xFFB84600, 0xF2221510, 0xE6161010, 0xE6100B09, 0xF01C130F, 0xFF3D2A1E),
	METEOR("Meteor",     0xFF913DE2, 0xFF5B2491, 0xF2201A2B, 0xE6141218, 0xE60F0D13, 0xF0191621, 0xFF2A2433),
	OCEAN("Ocean",       0xFF2E9BFF, 0xFF1A5A99, 0xF2141B26, 0xE60F141B, 0xE60B0F14, 0xF0121821, 0xFF223044),
	EMERALD("Emerald",   0xFF2ECC71, 0xFF1B7A43, 0xF2131F18, 0xE60E1611, 0xE60A100C, 0xF0111C15, 0xFF203A2B),
	CRIMSON("Crimson",   0xFFE53950, 0xFF8C1F2E, 0xF2241417, 0xE6181012, 0xE6110B0D, 0xF01E1215, 0xFF3D2228),
	SAKURA("Sakura",     0xFFFF7EB6, 0xFFA34C74, 0xF2241820, 0xE6181116, 0xE6110C10, 0xF01E151B, 0xFF3D2633),
	GOLD("Gold",         0xFFFFC72C, 0xFFA67C00, 0xF2221E12, 0xE616140D, 0xE6100E09, 0xF01C190F, 0xFF3D3520),
	MONO("Mono",         0xFFE0E0E0, 0xFF707070, 0xF21A1A1A, 0xE6121212, 0xE60C0C0C, 0xF0161616, 0xFF2E2E2E),
	CUSTOM("Custom", 0, 0, 0, 0, 0, 0, 0);

	private final String display;
	final int accent, accentDark, header, background, settingBackground, section, outline;

	ThemePreset(String display, int accent, int accentDark, int header, int background, int settingBackground, int section, int outline) {
		this.display = display;
		this.accent = accent;
		this.accentDark = accentDark;
		this.header = header;
		this.background = background;
		this.settingBackground = settingBackground;
		this.section = section;
		this.outline = outline;
	}

	@Override
	public String toString() {
		return display;
	}
}
