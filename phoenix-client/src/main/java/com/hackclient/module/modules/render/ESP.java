package com.hackclient.module.modules.render;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

/** Makes entities glow through walls. Hooked in MinecraftMixin. */
public class ESP extends Module {
	private final BoolSetting players = bool("Players", true);
	private final BoolSetting mobs = bool("Mobs", true);
	private final BoolSetting animals = bool("Animals", false);
	private final BoolSetting items = bool("Items", false);

	public ESP() {
		super("ESP", "Highlights entities through walls.", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	public boolean shouldGlow(Entity entity) {
		if (entity == mc.player) return false;
		if (entity instanceof Player) return players.get();
		if (entity instanceof Enemy) return mobs.get();
		if (entity instanceof Animal) return animals.get();
		if (entity instanceof ItemEntity) return items.get();
		return false;
	}
}
