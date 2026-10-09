package com.nullhours.client;

import com.nullhours.ModEntities;
import com.nullhours.NullHours;
import com.nullhours.net.ScarePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class NullHoursClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.HOLLOW, ctx -> new SpriteRenderer<>(ctx, "hollow", true));
		EntityRendererRegistry.register(ModEntities.ECHO, ctx -> new SpriteRenderer<>(ctx, "echo", false));
		EntityRendererRegistry.register(ModEntities.GRINNER, ctx -> new SpriteRenderer<>(ctx, "grinner", true));

		ClientPlayNetworking.registerGlobalReceiver(ScarePayload.TYPE, (payload, context) -> ScareOverlay.handle(payload));
		ClientTickEvents.END_CLIENT_TICK.register(client -> ScareOverlay.tick());
		HudElementRegistry.addLast(NullHours.id("scare_overlay"), ScareOverlay::render);
	}
}
