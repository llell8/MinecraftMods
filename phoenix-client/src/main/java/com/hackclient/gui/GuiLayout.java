package com.hackclient.gui;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/** Saved positions of the click GUI's category panels, keyed by category name. Stored in the config file. */
public final class GuiLayout {
	public record PanelState(int x, int y, boolean expanded) {}

	private static final Map<String, PanelState> PANELS = new HashMap<>();

	private GuiLayout() {}

	public static PanelState get(String category) {
		return PANELS.get(category);
	}

	public static void put(String category, PanelState state) {
		PANELS.put(category, state);
	}

	public static JsonObject toJson() {
		JsonObject json = new JsonObject();
		PANELS.forEach((category, state) -> {
			JsonObject panel = new JsonObject();
			panel.addProperty("x", state.x());
			panel.addProperty("y", state.y());
			panel.addProperty("expanded", state.expanded());
			json.add(category, panel);
		});
		return json;
	}

	public static void fromJson(JsonObject json) {
		PANELS.clear();
		for (String category : json.keySet()) {
			JsonObject panel = json.getAsJsonObject(category);
			PANELS.put(category, new PanelState(
					panel.get("x").getAsInt(),
					panel.get("y").getAsInt(),
					!panel.has("expanded") || panel.get("expanded").getAsBoolean()));
		}
	}
}
