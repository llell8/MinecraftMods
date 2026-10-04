package com.hackclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

/**
 * Mace smash attacks don't count while you're gliding. Sending "start gliding" while already gliding
 * makes the server stop your glide instead, keeping the fall distance from the dive, so a mace hit
 * sent right after lands as a smash.
 */
public final class ElytraUtil {
	private ElytraUtil() {}

	/** @return true if you were gliding and the glide was cancelled */
	public static boolean cancelGlide() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !mc.player.isFallFlying()) return false;
		mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
		mc.player.stopFallFlying();
		return true;
	}
}
