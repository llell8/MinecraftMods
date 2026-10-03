package com.hackclient.module.modules.combat;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import com.hackclient.util.TargetUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

public class TriggerBot extends Module {
	private final BoolSetting players = bool("Players", true);
	private final BoolSetting mobs = bool("Mobs", true);
	private final BoolSetting animals = bool("Animals", false);

	public TriggerBot() {
		super("TriggerBot", "Attacks the entity you're looking at.", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		Entity target = mc.crosshairPickEntity;
		if (target == null || mc.player.getAttackStrengthScale(0.5f) < 1f) return;
		if (!TargetUtil.isValidTarget(target, players.get(), mobs.get(), animals.get())) return;

		mc.gameMode.attack(mc.player, target);
		mc.player.swing(InteractionHand.MAIN_HAND);
	}
}
