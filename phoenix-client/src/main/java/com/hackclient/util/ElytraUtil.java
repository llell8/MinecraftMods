package com.hackclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

/**
 * Mace smash attacks don't count while you're gliding. Sending "start gliding" while already gliding
 * makes the server stop your glide instead, keeping the fall distance from the dive, so a mace hit
 * sent right after lands as a smash.
 *
 * Only worth doing in a real dive: while taking off or climbing the server keeps resetting your fall
 * distance, so cancelling there just drops you out of the glide for nothing.
 */
public final class ElytraUtil {
	private static final int MIN_GLIDE_TICKS = 10;
	private static final double MIN_DIVE_SPEED = 0.5; // blocks per tick downward

	private ElytraUtil() {}

	/** Gliding, past take-off, and heading down fast enough to have built up fall distance. */
	public static boolean isDiving() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.player.isFallFlying()
				&& mc.player.getFallFlyingTicks() >= MIN_GLIDE_TICKS
				&& mc.player.getDeltaMovement().y <= -MIN_DIVE_SPEED;
	}

	/** Stops the glide if you're diving. @return true if it did */
	public static boolean cancelGlide() {
		Minecraft mc = Minecraft.getInstance();
		if (!isDiving()) return false;
		mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
		mc.player.stopFallFlying();
		return true;
	}
}
