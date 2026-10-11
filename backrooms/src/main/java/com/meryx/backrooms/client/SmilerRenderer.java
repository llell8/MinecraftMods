package com.meryx.backrooms.client;

import com.meryx.backrooms.Backrooms;
import com.meryx.backrooms.entity.Smiler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public class SmilerRenderer extends MobRenderer<Smiler, LivingEntityRenderState, SmilerModel> {
    private static final Identifier TEXTURE = Backrooms.id("textures/entity/smiler.png");

    public SmilerRenderer(EntityRendererProvider.Context context) {
        super(context, new SmilerModel(context.bakeLayer(SmilerModel.LAYER)), 0.0F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    /** Fully bright: it is made of light. */
    @Override
    public int getBlockLightLevel(Smiler smiler, BlockPos pos) {
        return 15;
    }

    @Override
    public void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(SmilerModel.SCALE, SmilerModel.SCALE, SmilerModel.SCALE);
    }
}
