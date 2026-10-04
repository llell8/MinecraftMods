package com.hackclient.mixin;

import com.hackclient.HackClient;
import com.hackclient.module.modules.player.NoFall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerMixin {
	/**
	 * NoFall tells the server you're on the ground while falling. The server won't start an elytra glide
	 * while it thinks that, and cancels it instead (the "bounce"). So right before "start gliding",
	 * say we're in the air.
	 */
	@Inject(method = "send", at = @At("HEAD"))
	private void hackclient$send(Packet<?> packet, CallbackInfo ci) {
		if (!(packet instanceof ServerboundPlayerCommandPacket command)) return;
		if (command.getAction() != ServerboundPlayerCommandPacket.Action.START_FALL_FLYING) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || HackClient.getModuleManager().getIfEnabled(NoFall.class) == null) return;
		((ClientCommonPacketListenerImpl) (Object) this).send(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));
	}
}
