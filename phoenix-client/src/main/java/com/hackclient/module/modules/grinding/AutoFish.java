package com.hackclient.module.modules.grinding;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Reels in when a fish bites and casts again. Bite detection is hooked in ClientPacketListenerMixin. */
public class AutoFish extends Module {
	private final NumberSetting recastDelay = number("Recast delay", 15, 5, 60, 0);

	private int recastTimer = -1;

	public AutoFish() {
		super("AutoFish", "Catches fish automatically. Hold a fishing rod.", Category.GRINDING, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		recastTimer = -1;
	}

	@Override
	public void onTick() {
		if (recastTimer > 0 && --recastTimer == 0 && mc.player.fishing == null && mc.screen == null) {
			useRod();
		}
	}

	public void onSound(Holder<SoundEvent> sound, double x, double y, double z) {
		if (sound.value() != SoundEvents.FISHING_BOBBER_SPLASH || mc.player == null || mc.player.fishing == null) return;
		// Only react to our own bobber
		if (mc.player.fishing.distanceToSqr(x, y, z) > 4) return;

		useRod();
		recastTimer = recastDelay.get().intValue();
	}

	private void useRod() {
		InteractionHand hand;
		if (mc.player.getMainHandItem().is(Items.FISHING_ROD)) hand = InteractionHand.MAIN_HAND;
		else if (mc.player.getOffhandItem().is(Items.FISHING_ROD)) hand = InteractionHand.OFF_HAND;
		else return;

		mc.gameMode.useItem(mc.player, hand);
		mc.player.swing(hand);
	}
}
