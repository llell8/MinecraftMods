package com.hackclient.gui;

/** Selectable designs for GUI widgets (ClickGUI → Buttons). */
public final class Styles {
	private Styles() {}

	public enum Checkbox {
		BOX("Box"), SWITCH("Switch"), CHECK("Checkmark"), DOT("Dot");

		private final String display;

		Checkbox(String display) {
			this.display = display;
		}

		@Override
		public String toString() {
			return display;
		}
	}

	public enum Slider {
		LINE("Line"), BAR("Bar"), DOT("Dot");

		private final String display;

		Slider(String display) {
			this.display = display;
		}

		@Override
		public String toString() {
			return display;
		}
	}

	/** The little icon on module rows that have settings. */
	public enum SettingsIcon {
		DOTS("Dots"), ARROW("Arrow"), GEAR("Gear"), LINES("Lines"), NONE("None");

		private final String display;

		SettingsIcon(String display) {
			this.display = display;
		}

		@Override
		public String toString() {
			return display;
		}
	}

	public enum ModuleRow {
		BAR("Side bar"), FILL("Filled"), TEXT("Colored text");

		private final String display;

		ModuleRow(String display) {
			this.display = display;
		}

		@Override
		public String toString() {
			return display;
		}
	}
}
