package com.nullhours.haunt;

import com.nullhours.ModEntities;
import com.nullhours.NullHoursConfig;
import com.nullhours.entity.EchoEntity;
import com.nullhours.entity.GrinnerEntity;
import com.nullhours.entity.HollowEntity;
import com.nullhours.net.ScarePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.RotationSegment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything the haunting can do to a player. */
public final class HauntEvents {
	@FunctionalInterface
	interface Action {
		/** Returns false when the event could not happen here, so another one is picked. */
		boolean run(ServerLevel level, ServerPlayer player, int stage);
	}

	record Event(String name, int minStage, int weight, Action action) {
	}

	public static final Map<String, Event> EVENTS = new LinkedHashMap<>();

	private static final String[] SIGNS = {
			"TURN AROUND", "i was here", "dont sleep", "it follows you", "WAKE UP",
			"you are not alone", "why did you come back", "LEAVE"
	};
	private static final String[] FLASHES = {
			"IT SEES YOU", "WAKE UP", "DONT TURN AROUND", "YOU ARE NOT ALONE", "NULL", "BEHIND YOU"
	};

	static {
		add("footsteps", 0, 4, HauntEvents::footsteps);
		add("ambience", 0, 3, HauntEvents::ambience);
		add("chat", 1, 2, HauntEvents::chat);
		add("torches", 1, 3, HauntEvents::torches);
		add("door", 1, 2, HauntEvents::door);
		add("hollow", 1, 4, HauntEvents::hollowSighting);
		add("sign", 2, 2, HauntEvents::sign);
		add("echo", 2, 3, HauntEvents::echo);
		add("grinner", 2, 3, HauntEvents::grinner);
		add("glitch", 2, 2, HauntEvents::glitch);
		add("flash", 2, 1, HauntEvents::flash);
		add("darkness", 3, 2, HauntEvents::darkness);
		add("chase", 3, 2, HauntEvents::chase);
		add("blackout", 3, 1, HauntEvents::blackout);
	}

	private HauntEvents() {
	}

	private static void add(String name, int minStage, int weight, Action action) {
		EVENTS.put(name, new Event(name, minStage, weight, action));
	}

	/** Picks weighted events allowed at this stage until one of them works. */
	static void runRandom(ServerLevel level, ServerPlayer player, int stage) {
		List<Event> pool = new ArrayList<>();
		for (Event event : EVENTS.values()) if (event.minStage() <= stage) pool.add(event);
		RandomSource random = player.getRandom();
		for (int attempt = 0; attempt < 4 && !pool.isEmpty(); attempt++) {
			int total = pool.stream().mapToInt(Event::weight).sum();
			int roll = random.nextInt(total);
			Event picked = pool.getLast();
			for (Event event : pool) {
				roll -= event.weight();
				if (roll < 0) {
					picked = event;
					break;
				}
			}
			if (picked.action().run(level, player, stage)) return;
			pool.remove(picked);
		}
	}

	/** Runs an event by name, ignoring the stage. Returns false if it could not happen. */
	public static boolean run(String name, ServerLevel level, ServerPlayer player) {
		Event event = EVENTS.get(name);
		return event != null && event.action().run(level, player, 3);
	}

	private static boolean isDark(ServerLevel level, ServerPlayer player) {
		long time = level.getDayTime() % 24000L;
		return (time > 13000 && time < 23000) || level.getMaxLocalRawBrightness(player.blockPosition()) < 7;
	}

	private static void sound(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, SoundSource.HOSTILE, volume, pitch);
	}

	private static float behind(ServerPlayer player, RandomSource random, int spread) {
		return player.getYRot() + 180 + random.nextInt(spread * 2 + 1) - spread;
	}

	private static void send(ServerPlayer player, ScarePayload payload) {
		if (NullHoursConfig.INSTANCE.screenEffects || payload.kind() == ScarePayload.JUMPSCARE) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	// Sounds

	private static boolean footsteps(ServerLevel level, ServerPlayer player, int stage) {
		RandomSource random = player.getRandom();
		float yaw = behind(player, random, 20);
		int steps = 4 + random.nextInt(4);
		for (int i = 0; i < steps; i++) {
			double distance = 7.0 - i * 0.8;
			HauntDirector.schedule(i * 7, () -> {
				if (player.isRemoved()) return;
				BlockPos pos = SpawnUtil.around(level, player, yaw, distance, 2);
				if (pos == null) return;
				SoundEvent step = level.getBlockState(pos.below()).getSoundType().getStepSound();
				sound(level, pos, step, 0.6f, 0.9f);
			});
		}
		return true;
	}

	private static boolean ambience(ServerLevel level, ServerPlayer player, int stage) {
		RandomSource random = player.getRandom();
		BlockPos pos = player.blockPosition().offset(random.nextInt(17) - 8, random.nextInt(5) - 2, random.nextInt(17) - 8);
		level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.AMBIENT_CAVE, SoundSource.AMBIENT, 1.0f, 0.5f + random.nextFloat() * 0.3f);
		return true;
	}

	// The world

	private static boolean chat(ServerLevel level, ServerPlayer player, int stage) {
		if (!NullHoursConfig.INSTANCE.fakeChat) return false;
		RandomSource random = player.getRandom();
		switch (random.nextInt(3)) {
			case 0 -> {
				EchoEntity.joinMessage(player);
				HauntDirector.schedule(60 + random.nextInt(60), () -> EchoEntity.say(player, "hello " + player.getName().getString().toLowerCase()));
			}
			case 1 -> EchoEntity.say(player, random.nextBoolean() ? "where are you" : "i know where you sleep");
			default -> {
				EchoEntity.say(player, "...");
				HauntDirector.schedule(40, () -> player.sendSystemMessage(Component.translatable("multiplayer.player.left", EchoEntity.NAME)
						.withStyle(net.minecraft.ChatFormatting.YELLOW)));
			}
		}
		return true;
	}

	private static boolean torches(ServerLevel level, ServerPlayer player, int stage) {
		if (!NullHoursConfig.INSTANCE.breakTorches) return false;
		List<BlockPos> found = new ArrayList<>();
		BlockPos center = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-14, -5, -14), center.offset(14, 5, 14))) {
			BlockState state = level.getBlockState(pos);
			if (isTorch(state) && !SpawnUtil.isInView(player, pos, 0.4)) found.add(pos.immutable());
		}
		if (found.isEmpty()) return false;
		RandomSource random = player.getRandom();
		int count = Math.min(found.size(), 1 + stage);
		for (int i = 0; i < count; i++) {
			BlockPos pos = found.remove(random.nextInt(found.size()));
			HauntDirector.schedule(i * 15, () -> {
				if (isTorch(level.getBlockState(pos))) level.destroyBlock(pos, false);
			});
		}
		return true;
	}

	private static boolean isTorch(BlockState state) {
		return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH);
	}

	private static boolean door(ServerLevel level, ServerPlayer player, int stage) {
		BlockPos center = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-12, -3, -12), center.offset(12, 3, 12))) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof DoorBlock door && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
					&& !state.getValue(DoorBlock.OPEN) && pos.distSqr(center) > 4) {
				BlockPos doorPos = pos.immutable();
				door.setOpen(null, level, state, doorPos, true);
				HauntDirector.schedule(40 + player.getRandom().nextInt(60), () -> {
					BlockState now = level.getBlockState(doorPos);
					if (now.getBlock() instanceof DoorBlock d && now.getValue(DoorBlock.OPEN)) d.setOpen(null, level, now, doorPos, false);
				});
				return true;
			}
		}
		return false;
	}

	private static boolean sign(ServerLevel level, ServerPlayer player, int stage) {
		if (!NullHoursConfig.INSTANCE.placeSigns) return false;
		RandomSource random = player.getRandom();
		BlockPos pos = SpawnUtil.around(level, player, behind(player, random, 30), 3 + random.nextInt(3), 2);
		if (pos == null || !level.getBlockState(pos).isAir()) return false;
		float yaw = SpawnUtil.yawTowards(player.getX(), player.getZ(), pos.getX() + 0.5, pos.getZ() + 0.5);
		BlockState state = Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment(yaw + 180));
		level.setBlock(pos, state, 3);
		if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return false;
		String[] words = SIGNS[random.nextInt(SIGNS.length)].split(" ");
		SignText text = sign.getFrontText();
		// Spread the words over the middle lines, two per line
		for (int line = 0, w = 0; line < 4 && w < words.length; line++) {
			if (line == 0 && words.length <= 4) continue;
			String row = words[w++];
			if (w < words.length && row.length() + words[w].length() < 14) row += " " + words[w++];
			text = text.setMessage(line, Component.literal(row));
		}
		sign.setText(text, true);
		return true;
	}

	// Entities

	private static boolean hollowSighting(ServerLevel level, ServerPlayer player, int stage) {
		if (!isDark(level, player) || HauntDirector.hasActiveEntity(level, player)) return false;
		RandomSource random = player.getRandom();
		BlockPos pos = SpawnUtil.around(level, player, player.getYRot() + random.nextInt(141) - 70, 28 + random.nextInt(12), 3);
		if (pos == null) return false;
		HollowEntity hollow = SpawnUtil.spawn(ModEntities.HOLLOW, level, pos, player);
		if (hollow == null) return false;
		hollow.setChaseChance(stage <= 1 ? 0.0f : stage == 2 ? 0.25f : 0.6f);
		HauntDirector.setActiveEntity(player, hollow);
		return true;
	}

	private static boolean chase(ServerLevel level, ServerPlayer player, int stage) {
		if (!isDark(level, player) || HauntDirector.hasActiveEntity(level, player)) return false;
		BlockPos pos = SpawnUtil.around(level, player, behind(player, player.getRandom(), 40), 22, 3);
		if (pos == null) return false;
		HollowEntity hollow = SpawnUtil.spawn(ModEntities.HOLLOW, level, pos, player);
		if (hollow == null) return false;
		hollow.startChase(level);
		HauntDirector.setActiveEntity(player, hollow);
		return true;
	}

	private static boolean echo(ServerLevel level, ServerPlayer player, int stage) {
		if (HauntDirector.hasActiveEntity(level, player)) return false;
		BlockPos pos = SpawnUtil.around(level, player, behind(player, player.getRandom(), 60), 18 + player.getRandom().nextInt(8), 2);
		if (pos == null) return false;
		EchoEntity echo = SpawnUtil.spawn(ModEntities.ECHO, level, pos, player);
		if (echo == null) return false;
		EchoEntity.joinMessage(player);
		HauntDirector.setActiveEntity(player, echo);
		return true;
	}

	private static boolean grinner(ServerLevel level, ServerPlayer player, int stage) {
		if (HauntDirector.hasActiveEntity(level, player)) return false;
		BlockPos pos = SpawnUtil.around(level, player, player.getYRot() + 180, 2.5, 3);
		if (pos == null || SpawnUtil.isInView(player, pos, 0.3)) return false;
		GrinnerEntity grinner = SpawnUtil.spawn(ModEntities.GRINNER, level, pos, player);
		if (grinner == null) return false;
		HauntDirector.setActiveEntity(player, grinner);
		return true;
	}

	// Screen effects

	private static boolean glitch(ServerLevel level, ServerPlayer player, int stage) {
		send(player, ScarePayload.effect(ScarePayload.GLITCH, 20 + player.getRandom().nextInt(30), ""));
		return true;
	}

	private static boolean flash(ServerLevel level, ServerPlayer player, int stage) {
		RandomSource random = player.getRandom();
		String text = random.nextInt(5) == 0 ? player.getName().getString().toUpperCase() : FLASHES[random.nextInt(FLASHES.length)];
		send(player, ScarePayload.effect(ScarePayload.FLASH, 6, text));
		return true;
	}

	private static boolean blackout(ServerLevel level, ServerPlayer player, int stage) {
		send(player, ScarePayload.effect(ScarePayload.BLACKOUT, 70, "where are you going"));
		return true;
	}

	private static boolean darkness(ServerLevel level, ServerPlayer player, int stage) {
		player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 12, 0, false, false));
		sound(level, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, 1.0f, 0.8f);
		return true;
	}
}
