package com.nullhours.haunt;

import com.nullhours.NullHoursConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Decides when something happens to each player. Events start rare and quiet, and get
 * more frequent and more dangerous as the in-game days go by.
 */
public final class HauntDirector {
	private static final Map<UUID, State> STATES = new HashMap<>();
	private static final List<Scheduled> SCHEDULED = new ArrayList<>();

	private HauntDirector() {
	}

	static final class State {
		int cooldown = 20 * 60;
		UUID activeEntity;
	}

	private record Scheduled(long runAt, Runnable task) {
	}

	private static long now;

	public static void tick(MinecraftServer server) {
		now++;
		runScheduled();
		NullHoursConfig config = NullHoursConfig.INSTANCE;
		if (!config.enabled) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSpectator() || !player.isAlive()) continue;
			State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
			if (--state.cooldown > 0) continue;
			ServerLevel level = (ServerLevel) player.level();
			int stage = stage(level);
			state.cooldown = cooldown(stage, player.getRandom());
			HauntEvents.runRandom(level, player, stage);
		}
	}

	public static void reset() {
		STATES.clear();
		SCHEDULED.clear();
	}

	/** Runs a task a number of ticks from now. */
	public static void schedule(int delay, Runnable task) {
		SCHEDULED.add(new Scheduled(now + delay, task));
	}

	private static void runScheduled() {
		if (SCHEDULED.isEmpty()) return;
		List<Scheduled> due = new ArrayList<>();
		SCHEDULED.removeIf(s -> {
			if (s.runAt() > now) return false;
			due.add(s);
			return true;
		});
		for (Scheduled s : due) s.task().run();
	}

	public static int stage(ServerLevel level) {
		NullHoursConfig config = NullHoursConfig.INSTANCE;
		if (config.stageOverride >= 0) return Math.min(3, config.stageOverride);
		long day = level.getDayTime() / 24000L;
		if (day >= config.stage3Day) return 3;
		if (day >= config.stage2Day) return 2;
		if (day >= config.stage1Day) return 1;
		return 0;
	}

	private static int cooldown(int stage, RandomSource random) {
		int[] minSeconds = {240, 100, 60, 35};
		int min = minSeconds[stage];
		int seconds = min + random.nextInt(min + 1);
		double frequency = Math.max(0.05, NullHoursConfig.INSTANCE.eventFrequency);
		return Math.max(20, (int) (seconds * 20 / frequency));
	}

	/** True while an entity sent after this player is still around, so only one comes at a time. */
	static boolean hasActiveEntity(ServerLevel level, ServerPlayer player) {
		State state = STATES.get(player.getUUID());
		if (state == null || state.activeEntity == null) return false;
		Entity entity = level.getEntity(state.activeEntity);
		return entity != null && entity.isAlive();
	}

	static void setActiveEntity(ServerPlayer player, Entity entity) {
		STATES.computeIfAbsent(player.getUUID(), id -> new State()).activeEntity = entity.getUUID();
	}
}
