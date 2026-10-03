package com.hackclient.module.modules.combat;

import com.hackclient.HackClient;
import com.hackclient.mixin.MultiPlayerGameModeAccessor;
import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.NumberSetting;
import com.hackclient.setting.SlotsSetting;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

/**
 * Stun slam: when you hit someone who is blocking with a shield, hit them with an axe (disables the shield,
 * and since the hit was blocked they get no invulnerability frames), then with a mace in the same tick
 * so the smash attack lands in full. Both hits are sent back to back, then you swap back.
 *
 * Hooked in MultiPlayerGameModeMixin. The two hits go through attack() again, so MaceKill (fake fall)
 * and Criticals still apply to them; AttributeSwap is skipped while this runs.
 */
public class StunSlam extends Module {
	private final BoolSetting onlyShielding = bool("Only vs shields", true);
	private final BoolSetting requireFalling = bool("Require falling", true);
	private final NumberSetting minFall = number("Min fall distance", 1.5, 0, 10, 1);
	private final BoolSetting swapBack = bool("Swap back", true);
	private final SlotsSetting allowedSlots = slots("Use slots", SlotsSetting.ALL);

	private boolean running;

	public StunSlam() {
		super("StunSlam", "Axe breaks their shield, then a mace smash lands in the same tick.", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
	}

	/** True while our own two hits are being sent, so the mixin doesn't start another stun slam. */
	public boolean isRunning() {
		return running;
	}

	/** @return true if the stun slam was done (the original attack should be cancelled) */
	public boolean onAttack(Entity target) {
		if (running || !(target instanceof LivingEntity living)) return false;
		if (onlyShielding.get() && !living.isBlocking()) return false;
		if (mc.player.isFallFlying() || mc.player.isPassenger()) return false;

		// The smash needs a fall, unless MaceKill is on to fake one
		boolean maceKill = HackClient.getModuleManager().getIfEnabled(MaceKill.class) != null;
		if (requireFalling.get() && !maceKill && mc.player.fallDistance < minFall.get()) return false;

		int axe = findSlot(stack -> stack.is(ItemTags.AXES));
		int mace = findSlot(stack -> stack.is(Items.MACE));
		if (axe == -1 || mace == -1) return false;

		Inventory inventory = mc.player.getInventory();
		int previous = inventory.getSelectedSlot();
		running = true;
		try {
			// attack() sends the selected slot before each hit
			inventory.setSelectedSlot(axe);
			mc.gameMode.attack(mc.player, target);
			inventory.setSelectedSlot(mace);
			mc.gameMode.attack(mc.player, target);
		} finally {
			running = false;
		}
		mc.player.swing(InteractionHand.MAIN_HAND);

		if (swapBack.get() && previous != mace) {
			inventory.setSelectedSlot(previous);
			((MultiPlayerGameModeAccessor) mc.gameMode).hackclient$syncSelectedSlot();
		}
		return true;
	}

	private int findSlot(Predicate<ItemStack> predicate) {
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (allowedSlots.isEnabled(i) && predicate.test(inventory.getItem(i))) return i;
		}
		return -1;
	}
}
