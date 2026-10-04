package com.hackclient.module.modules.render;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.ModeSetting;
import com.hackclient.setting.NumberSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

/** Draws lines from the middle of your screen to entities. Drawn by EspOverlay. */
public class Tracers extends Module {
	private final BoolSetting players = bool("Players", true);
	private final BoolSetting mobs = bool("Mobs", false);
	private final BoolSetting animals = bool("Animals", false);
	private final BoolSetting items = bool("Items", false);
	private final NumberSetting maxDistance = number("Max distance", 256, 16, 512, 0);
	private final ModeSetting<Target> target = mode("Target", Target.BODY);
	private final NumberSetting lineWidth = number("Line width", 1.5, 0.5, 4, 1);
	private final BoolSetting distanceColors = bool("Distance colours", true);

	public enum Target {
		HEAD("Head"), BODY("Body"), FEET("Feet");

		private final String display;

		Target(String display) {
			this.display = display;
		}

		@Override
		public String toString() {
			return display;
		}
	}

	public Tracers() {
		super("Tracers", "Draws lines from your crosshair to players and other entities.", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	public boolean shouldTrace(Entity entity) {
		if (entity == mc.player || entity.distanceTo(mc.player) > maxDistance.get()) return false;
		if (entity instanceof Player) return players.get();
		if (entity instanceof Enemy) return mobs.get();
		if (entity instanceof Animal) return animals.get();
		if (entity instanceof ItemEntity) return items.get();
		return false;
	}

	/** Distance colours: red close to green far. Otherwise Meteor-style colours by type. */
	public int colorFor(Entity entity) {
		if (distanceColors.get()) return ESP.distanceColor(entity.distanceTo(mc.player));
		if (entity instanceof Player) return 0xFFFFFFFF;
		if (entity instanceof Enemy) return 0xFFFF1919;
		if (entity instanceof Animal) return 0xFF19FF19;
		return 0xFFFFA500;
	}

	/** How far up the entity the line ends, as a fraction of its height. */
	public double targetHeight() {
		return switch (target.get()) {
			case HEAD -> 0.9;
			case BODY -> 0.5;
			case FEET -> 0.0;
		};
	}

	public double lineWidth() {
		return lineWidth.get();
	}
}
