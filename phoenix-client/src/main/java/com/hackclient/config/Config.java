package com.hackclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hackclient.HackClient;
import com.hackclient.gui.GuiLayout;
import com.hackclient.module.Module;
import com.hackclient.module.ModuleManager;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.ModeSetting;
import com.hackclient.setting.MultiSetting;
import com.hackclient.setting.NumberSetting;
import com.hackclient.setting.Setting;
import com.hackclient.setting.SlotsSetting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Saves module state, keybinds, settings and the click GUI layout to config/hackclient.json. */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hackclient.json");
	/** Click GUI panel positions. Module names never start with '$', so this can't clash with one. */
	private static final String GUI_KEY = "$gui";

	private Config() {}

	public static void save(ModuleManager manager) {
		JsonObject root = new JsonObject();
		for (Module module : manager.getModules()) {
			JsonObject json = new JsonObject();
			json.addProperty("enabled", module.isEnabled());
			json.addProperty("key", module.getKey());

			JsonObject settings = new JsonObject();
			for (Setting<?> setting : module.getSettings()) {
				if (setting instanceof BoolSetting bool) settings.addProperty(setting.getName(), bool.get());
				else if (setting instanceof NumberSetting number) settings.addProperty(setting.getName(), number.get());
				else if (setting instanceof SlotsSetting slots) settings.addProperty(setting.getName(), slots.get());
				else if (setting instanceof ModeSetting<?> mode) settings.addProperty(setting.getName(), mode.get().name());
				else if (setting instanceof MultiSetting<?> multi) settings.addProperty(setting.getName(), multi.get());
			}
			json.add("settings", settings);
			root.add(module.getName(), json);
		}
		root.add(GUI_KEY, GuiLayout.toJson());

		try {
			Files.writeString(FILE, GSON.toJson(root));
		} catch (IOException e) {
			HackClient.LOGGER.error("Failed to save config", e);
		}
	}

	public static void load(ModuleManager manager) {
		if (!Files.exists(FILE)) return;

		try {
			JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
			if (root.has(GUI_KEY)) GuiLayout.fromJson(root.getAsJsonObject(GUI_KEY));
			for (Module module : manager.getModules()) {
				if (!root.has(module.getName())) continue;
				JsonObject json = root.getAsJsonObject(module.getName());

				if (json.has("key")) module.setKey(json.get("key").getAsInt());
				if (json.has("settings")) {
					JsonObject settings = json.getAsJsonObject("settings");
					for (Setting<?> setting : module.getSettings()) {
						JsonElement value = settings.get(setting.getName());
						if (value == null) continue;
						if (setting instanceof BoolSetting bool) bool.set(value.getAsBoolean());
						else if (setting instanceof NumberSetting number) number.set(value.getAsDouble());
						else if (setting instanceof SlotsSetting slots) slots.set(value.getAsInt());
						else if (setting instanceof ModeSetting<?> mode) mode.setByName(value.getAsString());
						else if (setting instanceof MultiSetting<?> multi) multi.set(value.getAsInt());
					}
				}
				if (json.has("enabled")) module.setEnabled(json.get("enabled").getAsBoolean());
			}
		} catch (Exception e) {
			HackClient.LOGGER.error("Failed to load config", e);
		}
	}
}
