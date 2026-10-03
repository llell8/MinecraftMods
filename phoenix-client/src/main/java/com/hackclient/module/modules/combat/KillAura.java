package com.hackclient.module.modules.combat;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.ModeSetting;
import com.hackclient.setting.MultiSetting;
import com.hackclient.setting.NumberSetting;
import com.hackclient.setting.SettingGroup;
import com.hackclient.util.ItemType;
import com.hackclient.util.TargetUtil;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KillAura extends Module {
	public enum Priority {
		CLOSEST("Closest"), LOWEST_HEALTH("Lowest health"), ANGLE("Crosshair");

		private final String display;

		Priority(String display) {
			this.display = display;
		}

		@Override
		public String toString() {
			return display;
		}
	}

	public enum Rotation {
		NONE("None"), SMOOTH("Smooth"), CLIENT("Snap"), SILENT("Silent");

		private final String display;

		Rotation(String display) {
			this.display = display;
		}

		@Override
		public String toString() {
			return display;
		}
	}

	private final SettingGroup sgGeneral = group("General", true);
	private final NumberSetting range = number("Range", 4.0, 1.0, 6.0, 1);
	private final NumberSetting wallsRange = number("Walls range", 3.0, 0.0, 6.0, 1);
	private final NumberSetting minCharge = number("Min charge %", 100, 0, 100, 0);
	private final ModeSetting<Rotation> rotation = mode("Rotation", Rotation.SMOOTH);

	private final SettingGroup sgTargets = group("Targets");
	private final BoolSetting players = bool("Players", true);
	private final BoolSetting hostile = bool("Hostile mobs", true);
	private final BoolSetting animals = bool("Animals", false);
	private final BoolSetting others = bool("Other mobs", false);
	private final ModeSetting<Priority> priority = mode("Priority", Priority.CLOSEST);
	private final NumberSetting maxTargets = number("Max targets", 1, 1, 5, 0);
	private final NumberSetting fov = number("FOV", 360, 10, 360, 0);

	private final SettingGroup sgIgnore = group("Ignore");
	private final BoolSetting ignoreCreative = bool("Ignore creative", true);
	private final BoolSetting ignoreInvisible = bool("Ignore invisible", false);
	private final BoolSetting ignoreNamed = bool("Ignore named", false);
	private final BoolSetting ignorePets = bool("Ignore pets", true);
	private final BoolSetting ignoreBabies = bool("Ignore babies", false);

	private final SettingGroup sgTiming = group("Timing");
	private final NumberSetting delay = number("Delay (ticks)", 0, 0, 20, 0);
	private final NumberSetting randomDelay = number("Random delay", 0, 0, 10, 0);
	private final BoolSetting waitForCrits = bool("Wait for crits", false);

	private final SettingGroup sgAim = group("Aiming");
	private final NumberSetting turnSpeed = number("Turn speed", 10, 1, 30, 0);
	private final NumberSetting aimTolerance = number("Aim tolerance", 25, 2, 90, 0);
	private final BoolSetting aimBetweenHits = bool("Aim between hits", true);

	private final SettingGroup sgWhen = group("When to work");
	private final MultiSetting<ItemType> heldItems = multi("Works while holding", ItemType.class, MultiSetting.maskOf(ItemType.class));
	private final BoolSetting pauseWhileUsing = bool("Pause while using", true);
	private final BoolSetting pauseInScreens = bool("Pause in menus", true);
	private final BoolSetting swing = bool("Swing hand", true);

	private int ticksSinceAttack;
	private int nextDelay;

	/** What Smooth rotation is turning towards; updated every tick, followed every frame. */
	private LivingEntity aimTarget;
	private long lastFrameNanos;

	public KillAura() {
		super("KillAura", "Attacks entities around you.", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onEnable() {
		ticksSinceAttack = 0;
		nextDelay = 0;
	}

	@Override
	protected void onDisable() {
		aimTarget = null;
	}

	@Override
	public void onTick() {
		ticksSinceAttack++;

		List<LivingEntity> targets = isPaused() ? List.of() : findTargets();
		boolean ready = !targets.isEmpty() && timingReady();
		boolean smooth = rotation.get() == Rotation.SMOOTH;

		aimTarget = smooth && !targets.isEmpty() && (ready || aimBetweenHits.get()) ? targets.getFirst() : null;
		if (!ready) return;

		// Smooth: only swing once the crosshair has turned close enough to the target
		if (smooth && TargetUtil.angleTo(targets.getFirst()) > aimTolerance.get()) return;

		for (LivingEntity target : targets) {
			attack(target);
		}

		ticksSinceAttack = 0;
		int random = randomDelay.get().intValue();
		nextDelay = delay.get().intValue() + (random > 0 ? ThreadLocalRandom.current().nextInt(random + 1) : 0);
	}

	/**
	 * Called every rendered frame (GameRendererMixin) so Smooth rotation turns at your frame rate instead of 20 times a second.
	 * Eases towards the target: fast when far off, slowing down as it lines up, like a real flick.
	 */
	public void onFrame() {
		long now = System.nanoTime();
		double seconds = Math.min((now - lastFrameNanos) / 1e9, 0.1);
		lastFrameNanos = now;

		if (aimTarget == null || mc.player == null || mc.screen != null) return;
		if (!aimTarget.isAlive() || aimTarget.isRemoved()) {
			aimTarget = null;
			return;
		}

		float[] goal = TargetUtil.rotationsTo(aimTarget);
		float yawDiff = Mth.wrapDegrees(goal[0] - mc.player.getYRot());
		float pitchDiff = goal[1] - mc.player.getXRot();
		double step = 1 - Math.exp(-turnSpeed.get() * seconds);

		// Entity#turn is what mouse movement uses (it scales by 0.15), so this looks exactly like turning your head
		mc.player.turn(yawDiff * step / 0.15, pitchDiff * step / 0.15);
	}

	private boolean isPaused() {
		if (pauseInScreens.get() && mc.screen != null) return true;
		if (pauseWhileUsing.get() && mc.player.isUsingItem()) return true;
		return !heldItems.isEnabled(ItemType.of(mc.player.getMainHandItem()));
	}

	private boolean timingReady() {
		if (ticksSinceAttack < nextDelay) return false;
		if (mc.player.getAttackStrengthScale(0.5f) * 100 < minCharge.get()) return false;

		// Crits need you to be falling: if you're in the air but still going up, wait
		if (waitForCrits.get() && !mc.player.onGround() && !mc.player.isInWater() && !mc.player.onClimbable()
				&& mc.player.fallDistance <= 0) {
			return false;
		}
		return true;
	}

	private void attack(LivingEntity target) {
		switch (rotation.get()) {
			case CLIENT -> TargetUtil.lookAt(target);
			case SILENT -> {
				float[] rotations = TargetUtil.rotationsTo(target);
				mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(rotations[0], rotations[1], mc.player.onGround(), mc.player.horizontalCollision));
			}
			case SMOOTH, NONE -> {} // Smooth already turned us in onFrame
		}

		mc.gameMode.attack(mc.player, target);
		if (swing.get()) mc.player.swing(InteractionHand.MAIN_HAND);
	}

	private List<LivingEntity> findTargets() {
		List<LivingEntity> targets = new ArrayList<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity instanceof LivingEntity living && isValid(living)) targets.add(living);
		}

		Comparator<LivingEntity> order = switch (priority.get()) {
			case CLOSEST -> Comparator.comparingDouble(TargetUtil::reachDistance);
			case LOWEST_HEALTH -> Comparator.comparingDouble(LivingEntity::getHealth);
			case ANGLE -> Comparator.comparingDouble(TargetUtil::angleTo);
		};
		targets.sort(order);

		int limit = maxTargets.get().intValue();
		return targets.size() > limit ? targets.subList(0, limit) : targets;
	}

	private boolean isValid(LivingEntity entity) {
		if (entity == mc.player || !entity.isAlive() || entity instanceof ArmorStand) return false;

		// Type filters
		if (entity instanceof Player player) {
			if (!players.get() || player.isSpectator()) return false;
			if (ignoreCreative.get() && player.isCreative()) return false;
		} else if (entity instanceof Enemy) {
			if (!hostile.get()) return false;
		} else if (entity instanceof Animal) {
			if (!animals.get()) return false;
		} else if (!others.get()) {
			return false;
		}

		// Ignore filters
		if (ignoreInvisible.get() && entity.isInvisible()) return false;
		if (ignoreNamed.get() && !(entity instanceof Player) && entity.hasCustomName()) return false;
		if (ignoreBabies.get() && entity.isBaby()) return false;
		if (ignorePets.get() && isPet(entity)) return false;

		// Range, walls and FOV
		double distance = TargetUtil.reachDistance(entity);
		if (distance > range.get()) return false;
		if (distance > wallsRange.get() && !mc.player.hasLineOfSight(entity)) return false;
		return fov.get() >= 360 || TargetUtil.angleTo(entity) <= fov.get() / 2;
	}

	private static boolean isPet(LivingEntity entity) {
		if (entity instanceof TamableAnimal tamable) return tamable.isTame();
		if (entity instanceof AbstractHorse horse) return horse.isTamed();
		return false;
	}
}
