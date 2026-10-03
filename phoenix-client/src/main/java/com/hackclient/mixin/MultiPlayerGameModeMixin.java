package com.hackclient.mixin;

import com.hackclient.HackClient;
import com.hackclient.module.ModuleManager;
import com.hackclient.module.modules.combat.AttributeSwap;
import com.hackclient.module.modules.combat.Criticals;
import com.hackclient.module.modules.combat.MaceKill;
import com.hackclient.module.modules.combat.StunSlam;
import com.hackclient.module.modules.grinding.ToolSaver;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	// Runs before attack() sends the selected slot and the attack packet
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void hackclient$attack(Player player, Entity target, CallbackInfo ci) {
		ModuleManager modules = HackClient.getModuleManager();

		// ToolSaver
		if (toolSaverBlocks()) {
			ci.cancel();
			return;
		}

		// StunSlam: sends its own axe + mace hits (which come back through here), so cancel the original
		StunSlam stunSlam = modules.getIfEnabled(StunSlam.class);
		boolean stunSlamRunning = stunSlam != null && stunSlam.isRunning();
		if (stunSlam != null && !stunSlamRunning && stunSlam.onAttack(target)) {
			ci.cancel();
			return;
		}

		// AttributeSwap (first, so MaceKill sees the mace if it swapped to one); not during a stun slam
		AttributeSwap attributeSwap = modules.getIfEnabled(AttributeSwap.class);
		if (attributeSwap != null && !stunSlamRunning) attributeSwap.preAttack(target);

		// MaceKill, otherwise Criticals
		MaceKill maceKill = modules.getIfEnabled(MaceKill.class);
		if (maceKill != null && maceKill.onAttack(target)) return;

		Criticals criticals = modules.getIfEnabled(Criticals.class);
		if (criticals != null) criticals.onAttack(target);
	}

	@Inject(method = "attack", at = @At("RETURN"))
	private void hackclient$afterAttack(Player player, Entity target, CallbackInfo ci) {
		AttributeSwap attributeSwap = HackClient.getModuleManager().getIfEnabled(AttributeSwap.class);
		if (attributeSwap != null) attributeSwap.postAttack();
	}

	// ToolSaver
	@Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
	private void hackclient$startDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (toolSaverBlocks()) cir.setReturnValue(false);
	}

	// ToolSaver
	@Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
	private void hackclient$continueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (toolSaverBlocks()) cir.setReturnValue(false);
	}

	private static boolean toolSaverBlocks() {
		ToolSaver toolSaver = HackClient.getModuleManager().getIfEnabled(ToolSaver.class);
		return toolSaver != null && toolSaver.shouldBlock();
	}
}
