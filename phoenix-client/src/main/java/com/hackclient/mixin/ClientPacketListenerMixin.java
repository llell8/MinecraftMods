package com.hackclient.mixin;

import com.hackclient.HackClient;
import com.hackclient.module.modules.grinding.AutoFish;
import com.hackclient.module.modules.grinding.TradePreview;
import com.hackclient.module.modules.movement.Velocity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	// Packet handlers first run on the network thread and get re-queued onto the main thread by this call.
	// Injecting right after it means our code only runs on the main thread.
	private static final String ON_MAIN_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V";

	// Velocity: knockback from hits
	@WrapOperation(method = "handleSetEntityMotion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;lerpMotion(Lnet/minecraft/world/phys/Vec3;)V"))
	private void hackclient$setEntityMotion(Entity entity, Vec3 motion, Operation<Void> original, ClientboundSetEntityMotionPacket packet) {
		Velocity velocity = HackClient.getModuleManager().getIfEnabled(Velocity.class);
		if (velocity != null && entity == Minecraft.getInstance().player) {
			motion = velocity.modify(motion);
		}
		original.call(entity, motion);
	}

	// Velocity: knockback from explosions
	@WrapOperation(method = "handleExplosion", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
	private void hackclient$explosionKnockback(Optional<Vec3> knockback, Consumer<Vec3> action, Operation<Void> original, ClientboundExplodePacket packet) {
		Velocity velocity = HackClient.getModuleManager().getIfEnabled(Velocity.class);
		original.call(velocity != null ? knockback.map(velocity::modify) : knockback, action);
	}

	// TradePreview: hide the trade screen while peeking
	@Inject(method = "handleOpenScreen", at = @At(value = "INVOKE", target = ON_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
	private void hackclient$openScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
		TradePreview tradePreview = HackClient.getModuleManager().getIfEnabled(TradePreview.class);
		if (tradePreview != null && tradePreview.onOpenScreen(packet)) ci.cancel();
	}

	// TradePreview: read the trades
	@Inject(method = "handleMerchantOffers", at = @At(value = "INVOKE", target = ON_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
	private void hackclient$merchantOffers(ClientboundMerchantOffersPacket packet, CallbackInfo ci) {
		TradePreview tradePreview = HackClient.getModuleManager().getIfEnabled(TradePreview.class);
		if (tradePreview != null && tradePreview.onMerchantOffers(packet)) ci.cancel();
	}

	// AutoFish: detect bites
	@Inject(method = "handleSoundEvent", at = @At(value = "INVOKE", target = ON_MAIN_THREAD, shift = At.Shift.AFTER))
	private void hackclient$sound(ClientboundSoundPacket packet, CallbackInfo ci) {
		AutoFish autoFish = HackClient.getModuleManager().getIfEnabled(AutoFish.class);
		if (autoFish != null) autoFish.onSound(packet.getSound(), packet.getX(), packet.getY(), packet.getZ());
	}
}
