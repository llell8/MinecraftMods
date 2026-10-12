package com.meryx.backrooms.client;

import com.meryx.backrooms.Backrooms;
import com.meryx.backrooms.entity.Bacteria;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class BacteriaRenderer extends MobRenderer<Bacteria, LivingEntityRenderState, BacteriaModel> {
    private static final Identifier TEXTURE = Backrooms.id("textures/entity/bacteria.png");

    public BacteriaRenderer(EntityRendererProvider.Context context) {
        super(context, new BacteriaModel(context.bakeLayer(BacteriaModel.LAYER)), 0.6F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
