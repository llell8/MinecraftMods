package com.hackclient.mixin;

import com.hackclient.HackClient;
import com.hackclient.module.modules.combat.AimAssist;
import com.hackclient.module.modules.combat.KillAura;
import net.minecraft.client.DeltaTracker;
import com.hackclient.module.modules.render.NoHurtCam;
import com.hackclient.module.modules.render.Zoom;
import com.hackclient.render.Projection;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	// KillAura smooth rotation + AimAssist (run once per frame)
	@Inject(method = "render", at = @At("HEAD"))
	private void hackclient$render(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		KillAura killAura = HackClient.getModuleManager().getIfEnabled(KillAura.class);
		if (killAura != null) killAura.onFrame();
		AimAssist aimAssist = HackClient.getModuleManager().getIfEnabled(AimAssist.class);
		if (aimAssist != null) aimAssist.onFrame();
	}

	// Zoom
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void hackclient$getFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
		Zoom zoom = HackClient.getModuleManager().getIfEnabled(Zoom.class);
		if (zoom != null) cir.setReturnValue(zoom.modifyFov(cir.getReturnValueF()));
		// Remember the world FOV for the ESP overlay's projection
		if (useFovSetting) Projection.setFov(cir.getReturnValueF());
	}

	// NoHurtCam
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void hackclient$bobHurt(PoseStack poseStack, float partialTick, CallbackInfo ci) {
		if (HackClient.getModuleManager().getIfEnabled(NoHurtCam.class) != null) ci.cancel();
	}
}
