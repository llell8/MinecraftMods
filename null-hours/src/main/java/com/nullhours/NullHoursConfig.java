package com.nullhours;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings stored in config/nullhours.json. */
public class NullHoursConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("nullhours.json");

	public static NullHoursConfig INSTANCE = new NullHoursConfig();

	/** Turns the whole haunting on or off. */
	public boolean enabled = true;
	/** Forces a stage from 0 to 3; -1 picks it from the number of days played. */
	public int stageOverride = -1;
	/** In-game days before stages 1, 2 and 3 begin. */
	public int stage1Day = 1;
	public int stage2Day = 2;
	public int stage3Day = 4;
	/** Higher means events happen more often; 2.0 is twice as often. */
	public double eventFrequency = 1.0;
	/** Damage dealt by a jumpscare, in half hearts. 0 makes them harmless. */
	public float jumpscareDamage = 4.0f;
	public boolean fakeChat = true;
	public boolean breakTorches = true;
	public boolean placeSigns = true;
	public boolean screenEffects = true;

	public static void load() {
		if (Files.exists(PATH)) {
			try {
				NullHoursConfig loaded = GSON.fromJson(Files.readString(PATH), NullHoursConfig.class);
				if (loaded != null) INSTANCE = loaded;
			} catch (Exception e) {
				NullHours.LOGGER.warn("Could not read {}, using defaults", PATH, e);
			}
		}
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(INSTANCE));
		} catch (IOException e) {
			NullHours.LOGGER.warn("Could not write {}", PATH, e);
		}
	}
}
