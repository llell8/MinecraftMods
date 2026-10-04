package com.hackclient.module.modules.render;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
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
	private final NumberSetting thickness = number("Thickness", 1, 1, 4, 0);

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

	/** Players: red close, yellow mid, green far. Others: fixed colours by type. */
	public int colorFor(Entity entity) {
		if (entity instanceof Player) {
			double distance = entity.distanceTo(mc.player);
			if (distance < 16) return 0xFFFF5555;
			if (distance < 48) return 0xFFFFFF55;
			return 0xFF55FF55;
		}
		if (entity instanceof Enemy) return 0xFFFF8800;
		if (entity instanceof Animal) return 0xFF55FFFF;
		return 0xFFFFFFFF;
	}

	public int thickness() {
		return thickness.get().intValue();
	}
}
