package com.meryx.backrooms.client;

import com.meryx.backrooms.Backrooms;
import com.meryx.backrooms.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;

public class BackroomsClient implements ClientModInitializer {
    private HumSound hum;

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(SmilerModel.LAYER, SmilerModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.SMILER, SmilerRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    /** Keeps the fluorescent hum playing while the player is in Level 0. */
    private void tick(Minecraft client) {
        if (client.level == null || client.player == null || !client.level.dimension().equals(Backrooms.LEVEL_0)) {
            return;
        }
        if (hum == null || hum.isStopped() || !client.getSoundManager().isActive(hum)) {
            hum = new HumSound();
            client.getSoundManager().play(hum);
        }
    }
}
