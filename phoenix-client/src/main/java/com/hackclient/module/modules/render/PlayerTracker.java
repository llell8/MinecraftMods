package com.hackclient.module.modules.render;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.NumberSetting;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lists the players your client knows about (the ones inside the server's tracking range) with their
 * coordinates, distance and which way they are from you. Drawn by PlayerTrackerHud.
 */
public class PlayerTracker extends Module {
	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

	private final NumberSetting maxPlayers = number("Max players", 10, 1, 30, 0);
	private final BoolSetting showCoords = bool("Coordinates", true);
	private final BoolSetting showDistance = bool("Distance", true);
	private final BoolSetting showDirection = bool("Direction arrow", true);
	private final BoolSetting showHealth = bool("Health", true);

	public PlayerTracker() {
		super("PlayerTracker", "Lists nearby players with their coordinates and distance.", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	/** One line of the HUD list. */
	public record Entry(String text, double distance) {
	}

	public List<Entry> entries() {
		List<Entry> result = new ArrayList<>();
		if (mc.level == null || mc.player == null) return result;

		List<AbstractClientPlayer> players = new ArrayList<>(mc.level.players());
		players.remove(mc.player);
		players.sort(Comparator.comparingDouble(p -> p.distanceTo(mc.player)));

		for (AbstractClientPlayer player : players) {
			if (result.size() >= maxPlayers.get().intValue()) break;
			double distance = player.distanceTo(mc.player);
			StringBuilder text = new StringBuilder();
			if (showDirection.get()) text.append(arrowTo(player)).append(' ');
			text.append(player.getName().getString());
			if (showHealth.get()) text.append(String.format(" %.0f❤", player.getHealth()));
			if (showCoords.get()) {
				text.append(String.format(" %d %d %d", player.getBlockX(), player.getBlockY(), player.getBlockZ()));
			}
			if (showDistance.get()) text.append(String.format(" (%.0fm)", distance));
			result.add(new Entry(text.toString(), distance));
		}
		return result;
	}

	/** An arrow pointing toward the player, relative to where you're facing. */
	private String arrowTo(AbstractClientPlayer player) {
		double dx = player.getX() - mc.player.getX();
		double dz = player.getZ() - mc.player.getZ();
		// Same convention as yaw: 0 = south (+Z), 90 = west (-X); turning right increases it
		float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float relative = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
		int index = Math.floorMod(Math.round(relative / 45f), 8);
		return ARROWS[index];
	}
}
