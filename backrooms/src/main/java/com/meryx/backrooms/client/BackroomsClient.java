package com.meryx.backrooms.client;

import com.meryx.backrooms.Backrooms;
import com.meryx.backrooms.block.ModBlocks;
import com.meryx.backrooms.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class BackroomsClient implements ClientModInitializer {
    private HumSound hum;

    @Override
    public void onInitializeClient() {
        // The glitch texture is mostly see-through, so the block underneath shows through it
        BlockRenderLayerMap.putBlock(ModBlocks.GLITCH, ChunkSectionLayer.TRANSLUCENT);
        EntityModelLayerRegistry.registerModelLayer(SmilerModel.LAYER, SmilerModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(BacteriaModel.LAYER, BacteriaModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.SMILER, context -> new SmilerRenderer(context, "smiler"));
        EntityRendererRegistry.register(ModEntities.VHS_SMILER, context -> new SmilerRenderer(context, "vhs_smiler"));
        EntityRendererRegistry.register(ModEntities.BACTERIA, BacteriaRenderer::new);
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
