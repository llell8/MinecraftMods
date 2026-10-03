package com.hackclient.mixin;

import com.hackclient.HackClient;
import com.hackclient.module.modules.combat.AttributeSwap;
import com.hackclient.module.modules.grinding.TradePreview;
import com.hackclient.module.modules.render.ESP;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
	private void hackclient$createTitle(CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(HackClient.NAME + " | " + cir.getReturnValue());
	}

	// AttributeSwap: spear reach / lunge dash
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void hackclient$startAttack(CallbackInfoReturnable<Boolean> cir) {
		AttributeSwap attributeSwap = HackClient.getModuleManager().getIfEnabled(AttributeSwap.class);
		if (attributeSwap != null && attributeSwap.onStartAttack()) cir.setReturnValue(true);
	}

	// ESP + TradePreview book highlighting
	@Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
	private void hackclient$shouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		ESP esp = HackClient.getModuleManager().getIfEnabled(ESP.class);
		if (esp != null && esp.shouldGlow(entity)) cir.setReturnValue(true);

		TradePreview tradePreview = HackClient.getModuleManager().getIfEnabled(TradePreview.class);
		if (tradePreview != null && tradePreview.shouldGlow(entity)) cir.setReturnValue(true);
	}
}
