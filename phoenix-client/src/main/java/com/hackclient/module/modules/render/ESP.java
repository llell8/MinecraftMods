package com.hackclient.module.modules.render;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.render.ShapeMode;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.ColorSetting;
import com.hackclient.setting.ModeSetting;
import com.hackclient.setting.NumberSetting;
import com.hackclient.setting.SettingGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

/**
 * Meteor-style entity ESP. Outline: traces the entity's actual model, armour and all, through walls
 * (Minecraft's glowing outline, in your colour; hooked in MinecraftMixin and EntityMixin).
 * Box: a 3D box around each entity. 2D: a flat rectangle. Boxes are drawn by EspOverlay.
 */
public class ESP extends Module {
	public enum Mode {
		OUTLINE("Outline"), BOX("Box"), FLAT("2D");

		private final String display;

		Mode(String display) {
			this.display = display;
		}

		@Override
		public String toString() {
			return display;
		}
	}

	private final SettingGroup sgGeneral = group("General", true);
	private final ModeSetting<Mode> mode = mode("Mode", Mode.OUTLINE);
	private final ModeSetting<ShapeMode> shapeMode = mode("Shape", ShapeMode.BOTH);
	private final NumberSetting fillOpacity = number("Fill opacity", 50, 0, 255, 0);
	private final NumberSetting lineWidth = number("Line width", 1.5, 0.5, 4, 1);
	private final NumberSetting maxDistance = number("Max distance", 256, 16, 512, 0);

	private final SettingGroup sgEntities = group("Entities", true);
	private final BoolSetting players = bool("Players", true);
	private final BoolSetting mobs = bool("Mobs", true);
	private final BoolSetting animals = bool("Animals", false);
	private final BoolSetting items = bool("Items", false);

	private final SettingGroup sgColors = group("Colours", false);
	private final BoolSetting distanceColors = bool("Distance colours", false);
	private final ColorSetting playerColor = color("Players", 0xFFFFFFFF);
	private final ColorSetting mobColor = color("Mobs", 0xFFFF1919);
	private final ColorSetting animalColor = color("Animals", 0xFF19FF19);
	private final ColorSetting itemColor = color("Items", 0xFFFFA500);

	public ESP() {
		super("ESP", "Highlights entities through walls.", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	private boolean typeEnabled(Entity entity) {
		if (entity == mc.player) return false;
		if (entity instanceof Player) return players.get();
		if (entity instanceof Enemy) return mobs.get();
		if (entity instanceof Animal) return animals.get();
		if (entity instanceof ItemEntity) return items.get();
		return false;
	}

	/** Used by MinecraftMixin and EntityMixin: only in Outline mode. */
	public boolean shouldGlow(Entity entity) {
		return mode.get() == Mode.OUTLINE && typeEnabled(entity) && entity.distanceTo(mc.player) <= maxDistance.get();
	}

	/** Used by EspOverlay: Box and 2D modes. */
	public boolean shouldDraw(Entity entity) {
		return mode.get() != Mode.OUTLINE && typeEnabled(entity) && entity.distanceTo(mc.player) <= maxDistance.get();
	}

	public Mode mode() {
		return mode.get();
	}

	public ShapeMode shapeMode() {
		return shapeMode.get();
	}

	public double lineWidth() {
		return lineWidth.get();
	}

	/** The colour picked for this entity type (defaults are Meteor's), or a distance colour. */
	public int lineColor(Entity entity) {
		if (distanceColors.get()) return distanceColor(entity.distanceTo(mc.player));
		if (entity instanceof Player) return playerColor.get();
		if (entity instanceof Enemy) return mobColor.get();
		if (entity instanceof Animal) return animalColor.get();
		if (entity instanceof ItemEntity) return itemColor.get();
		return 0xFFAFAFAF;
	}

	public int sideColor(Entity entity) {
		return (lineColor(entity) & 0x00FFFFFF) | (fillOpacity.get().intValue() << 24);
	}

	/** Red when close, through yellow, to green when far. */
	static int distanceColor(double distance) {
		double t = Math.max(0, Math.min(1, distance / 64));
		int r = (int) (255 * Math.min(1, 2 * (1 - t)));
		int g = (int) (255 * Math.min(1, 2 * t));
		return 0xFF000000 | (r << 16) | (g << 8);
	}
}
