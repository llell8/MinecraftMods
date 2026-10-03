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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;

/**
 * Gently pulls your crosshair towards a nearby target while you aim yourself. Doesn't attack.
 * The pull runs every frame (GameRendererMixin) using the same turning as mouse movement, so it looks natural.
 */
public class AimAssist extends Module {
	private final SettingGroup sgGeneral = group("General");
	private final NumberSetting range = number("Range", 4.5, 1.0, 8.0, 1);
	private final NumberSetting fov = number("FOV", 60, 10, 180, 0);
	private final NumberSetting speed = number("Strength", 4, 1, 20, 0);
	private final BoolSetting vertical = bool("Aim vertically", true);
	private final BoolSetting onlyWhileAttacking = bool("Only while attacking", false);
	private final BoolSetting stopOnTarget = bool("Stop when on target", true);
	private final BoolSetting throughWalls = bool("Through walls", false);

	private final SettingGroup sgTargets = group("Targets");
	private final BoolSetting players = bool("Players", true);
	private final BoolSetting hostile = bool("Hostile mobs", true);
	private final BoolSetting animals = bool("Animals", false);
	private final ModeSetting<KillAura.Priority> priority = mode("Priority", KillAura.Priority.ANGLE);

	private final SettingGroup sgWhen = group("When to work");
	private final MultiSetting<ItemType> heldItems = multi("Works while holding", ItemType.class, MultiSetting.maskOf(ItemType.class));

	private LivingEntity target;
	private long lastFrameNanos;

	public AimAssist() {
		super("AimAssist", "Helps your crosshair stick to targets.", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		target = null;
	}

	@Override
	public void onTick() {
		target = null;
		if (!heldItems.isEnabled(ItemType.of(mc.player.getMainHandItem()))) return;

		Comparator<LivingEntity> order = switch (priority.get()) {
			case CLOSEST -> Comparator.comparingDouble(TargetUtil::reachDistance);
			case LOWEST_HEALTH -> Comparator.comparingDouble(LivingEntity::getHealth);
			case ANGLE -> Comparator.comparingDouble(TargetUtil::angleTo);
		};

		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand) continue;
			if (!TargetUtil.isValidTarget(living, players.get(), hostile.get(), animals.get())) continue;
			if (TargetUtil.reachDistance(living) > range.get()) continue;
			if (TargetUtil.angleTo(living) > fov.get() / 2) continue;
			if (!throughWalls.get() && !mc.player.hasLineOfSight(living)) continue;
			if (target == null || order.compare(living, target) < 0) target = living;
		}
	}

	/** Called every rendered frame. */
	public void onFrame() {
		long now = System.nanoTime();
		double seconds = Math.min((now - lastFrameNanos) / 1e9, 0.1);
		lastFrameNanos = now;

		if (target == null || mc.player == null || mc.screen != null) return;
		if (!target.isAlive() || target.isRemoved()) {
			target = null;
			return;
		}
		if (onlyWhileAttacking.get() && !mc.options.keyAttack.isDown()) return;
		if (stopOnTarget.get() && mc.crosshairPickEntity == target) return;

		float[] goal = TargetUtil.rotationsTo(target);
		float yawDiff = Mth.wrapDegrees(goal[0] - mc.player.getYRot());
		float pitchDiff = vertical.get() ? goal[1] - mc.player.getXRot() : 0;
		double step = 1 - Math.exp(-speed.get() * seconds);

		// Entity#turn is what mouse movement uses (it scales by 0.15)
		mc.player.turn(yawDiff * step / 0.15, pitchDiff * step / 0.15);
	}
}
