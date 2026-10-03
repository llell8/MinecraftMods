package com.hackclient.module.modules.combat;

import com.hackclient.HackClient;
import com.hackclient.mixin.MultiPlayerGameModeAccessor;
import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.NumberSetting;
import com.hackclient.setting.SettingGroup;
import com.hackclient.setting.SlotsSetting;
import com.hackclient.util.ItemUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

/**
 * Swaps to a better hotbar item for the instant you hit, then swaps back.
 * The server uses the swapped item's damage/enchants but your current attack cooldown,
 * so e.g. a full-charge sword swing can land with an axe (shield break) or a mace (smash).
 *
 * Spears: a spear jab is its own attack (longer reach, hits everything in a line), so the
 * spear features hook the attack key instead (Minecraft#startAttack via MinecraftMixin).
 * Normal hits are hooked in MultiPlayerGameModeMixin.
 */
public class AttributeSwap extends Module {
	private final SettingGroup sgGeneral = group("General", true);
	private final BoolSetting smart = bool("Smart (pick best)", true);
	private final NumberSetting slot = number("Slot (if not smart)", 1, 1, 9, 0);
	private final BoolSetting swapBack = bool("Swap back", true);
	private final NumberSetting swapBackDelay = number("Swap back delay", 0, 0, 20, 0);

	private final SettingGroup sgSmart = group("Smart picking");
	private final BoolSetting shieldBreaker = bool("Axe vs shields", true);
	private final BoolSetting maceWhenFalling = bool("Mace when falling", true);
	private final BoolSetting useEnchants = bool("Count enchants", true);
	private final NumberSetting minDurability = number("Min durability", 10, 0, 100, 0);

	private final SettingGroup sgSlots = group("Slots");
	private final SlotsSetting toSlots = slots("Swap to slots", SlotsSetting.ALL);
	private final SlotsSetting fromSlots = slots("Swap from slots", SlotsSetting.ALL);
	private final BoolSetting onlyFromWeapons = bool("Only from weapons", false);

	private final SettingGroup sgSpears = group("Spears");
	private final BoolSetting spearReach = bool("Spear reach", true);
	private final BoolSetting lungeDash = bool("Lunge dash", false);
	private final BoolSetting lungeOnlySprinting = bool("Lunge only sprinting", true);

	private int previousSlot = -1;
	private int backTimer;

	public AttributeSwap() {
		super("AttributeSwap", "Hits with your best hotbar item, then swaps back.", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		if (previousSlot != -1 && mc.player != null) swapBackNow();
		previousSlot = -1;
	}

	@Override
	public void onTick() {
		if (previousSlot != -1 && backTimer > 0 && --backTimer == 0) swapBackNow();
	}

	// ---- Normal hits ----

	/** Called right before a normal attack packet is sent. */
	public void preAttack(Entity target) {
		if (previousSlot != -1 || !(target instanceof LivingEntity living) || !canSwapFrom()) return;

		int best = smart.get() ? findBestSlot(living) : manualSlot();
		if (best == -1 || best == mc.player.getInventory().getSelectedSlot()) return;
		swapTo(best);
	}

	/** Called right after the attack packet is sent. With no delay we swap back before the server's next tick. */
	public void postAttack() {
		if (previousSlot != -1 && backTimer == 0) swapBackNow();
	}

	// ---- Spears ----

	/** Called when you press attack. @return true if we did a spear jab instead of the normal click. */
	public boolean onStartAttack() {
		if (previousSlot != -1 || mc.missTime > 0 || mc.hitResult == null || mc.player.isHandsBusy() || mc.player.isSpectator()) return false;

		ItemStack held = mc.player.getMainHandItem();
		if (held.has(DataComponents.PIERCING_WEAPON) || !canSwapFrom()) return false; // already a spear: vanilla handles it
		if (mc.player.cannotAttackWithItem(held, 0)) return false;

		// Spear reach: nothing hittable in normal reach, but a spear could reach someone
		if (spearReach.get() && mc.hitResult.getType() != HitResult.Type.ENTITY) {
			for (int i = 0; i < 9; i++) {
				ItemStack stack = mc.player.getInventory().getItem(i);
				if (!ItemUtil.isSpear(stack) || !usable(i, stack)) continue;

				AttackRange range = stack.getOrDefault(DataComponents.ATTACK_RANGE, AttackRange.defaultFor(mc.player));
				HitResult hit = range.getClosesetHit(mc.player, 1.0f, this::isSpearTarget);
				if (hit instanceof EntityHitResult) {
					jab(i);
					return true;
				}
			}
		}

		// Lunge dash: clicking the air with a Lunge spear in the hotbar dashes you forward
		if (lungeDash.get() && mc.hitResult.getType() == HitResult.Type.MISS && (!lungeOnlySprinting.get() || mc.player.isSprinting())) {
			int best = -1;
			int bestLevel = 0;
			for (int i = 0; i < 9; i++) {
				ItemStack stack = mc.player.getInventory().getItem(i);
				if (!ItemUtil.isSpear(stack) || !usable(i, stack)) continue;
				int lunge = level(stack, Enchantments.LUNGE);
				if (lunge > bestLevel) {
					bestLevel = lunge;
					best = i;
				}
			}
			if (best != -1) {
				jab(best);
				return true;
			}
		}
		return false;
	}

	private void jab(int spearSlot) {
		swapTo(spearSlot);
		PiercingWeapon weapon = mc.player.getMainHandItem().get(DataComponents.PIERCING_WEAPON);
		mc.gameMode.piercingAttack(weapon); // sends the new slot, then the stab
		mc.player.swing(InteractionHand.MAIN_HAND);
		postAttack();
	}

	private boolean isSpearTarget(Entity entity) {
		return entity instanceof LivingEntity && entity != mc.player && PiercingWeapon.canHitEntity(mc.player, entity);
	}

	// ---- Swapping ----

	private void swapTo(int slot) {
		Inventory inventory = mc.player.getInventory();
		int current = inventory.getSelectedSlot();
		inventory.setSelectedSlot(slot);
		if (swapBack.get()) {
			previousSlot = current;
			backTimer = swapBackDelay.get().intValue();
		}
	}

	private void swapBackNow() {
		mc.player.getInventory().setSelectedSlot(previousSlot);
		((MultiPlayerGameModeAccessor) mc.gameMode).hackclient$syncSelectedSlot();
		previousSlot = -1;
	}

	private boolean canSwapFrom() {
		if (!fromSlots.isEnabled(mc.player.getInventory().getSelectedSlot())) return false;
		return !onlyFromWeapons.get() || ItemUtil.isWeapon(mc.player.getMainHandItem());
	}

	private int manualSlot() {
		int index = slot.get().intValue() - 1;
		return usable(index, mc.player.getInventory().getItem(index)) ? index : -1;
	}

	// ---- Picking the best item ----

	private int findBestSlot(LivingEntity target) {
		Inventory inventory = mc.player.getInventory();

		// Axes disable shields
		if (shieldBreaker.get() && target.isBlocking()) {
			int axe = findSlot(stack -> stack.is(ItemTags.AXES));
			if (axe != -1) return axe;
		}

		// Mace smash attacks scale with fall distance
		boolean falling = mc.player.fallDistance > 1.5 && !mc.player.isFallFlying();
		boolean maceKill = HackClient.getModuleManager().getIfEnabled(MaceKill.class) != null;
		if (maceWhenFalling.get() && (falling || maceKill)) {
			int mace = findSlot(stack -> stack.is(Items.MACE));
			if (mace != -1) return mace;
		}

		int best = -1;
		double bestScore = score(inventory.getItem(inventory.getSelectedSlot()), target);
		for (int i = 0; i < 9; i++) {
			ItemStack stack = inventory.getItem(i);
			if (!usable(i, stack) || ItemUtil.isSpear(stack)) continue; // spears can't do normal hits
			double score = score(stack, target);
			if (score > bestScore) {
				bestScore = score;
				best = i;
			}
		}
		return best;
	}

	/** Rough damage estimate for hitting the target with this item. */
	private double score(ItemStack stack, LivingEntity target) {
		if (stack.isEmpty()) return 1;

		double damage = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
				.compute(Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND);
		if (!useEnchants.get()) return damage;

		int sharpness = level(stack, Enchantments.SHARPNESS);
		if (sharpness > 0) damage += 0.5 * sharpness + 0.5;
		if (target.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)) damage += 2.5 * level(stack, Enchantments.SMITE);
		if (target.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) damage += 2.5 * level(stack, Enchantments.BANE_OF_ARTHROPODS);
		if (!target.isOnFire() && !target.fireImmune() && level(stack, Enchantments.FIRE_ASPECT) > 0) damage += 3;
		return damage;
	}

	/** Slot is allowed by "Swap to slots" and the item isn't about to break. */
	private boolean usable(int index, ItemStack stack) {
		if (!toSlots.isEnabled(index)) return false;
		return !stack.isDamageableItem() || stack.getMaxDamage() - stack.getDamageValue() > minDurability.get();
	}

	private int findSlot(Predicate<ItemStack> predicate) {
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < 9; i++) {
			ItemStack stack = inventory.getItem(i);
			if (predicate.test(stack) && usable(i, stack)) return i;
		}
		return -1;
	}



	private int level(ItemStack stack, ResourceKey<Enchantment> enchantment) {
		return mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantment)
				.map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
				.orElse(0);
	}
}
