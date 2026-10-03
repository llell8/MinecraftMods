package com.hackclient.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
	/** Sends the selected hotbar slot to the server right now instead of next tick. */
	@Invoker("ensureHasSentCarriedItem")
	void hackclient$syncSelectedSlot();
}
