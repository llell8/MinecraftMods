package com.hackclient.mixin;

import com.hackclient.HackClient;
import com.hackclient.module.modules.render.ESP;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
	// ESP Outline mode: the glowing outline uses this colour
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void hackclient$getTeamColor(CallbackInfoReturnable<Integer> cir) {
		ESP esp = HackClient.getModuleManager().getIfEnabled(ESP.class);
		Entity self = (Entity) (Object) this;
		if (esp != null && esp.shouldGlow(self)) cir.setReturnValue(esp.lineColor(self) & 0x00FFFFFF);
	}
}
