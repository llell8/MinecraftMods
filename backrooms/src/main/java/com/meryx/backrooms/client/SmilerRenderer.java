package com.meryx.backrooms.client;

import com.meryx.backrooms.Backrooms;
import com.meryx.backrooms.entity.Smiler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** A zombie-shaped silhouette, stretched tall and thin, with a grin. */
public class SmilerRenderer extends HumanoidMobRenderer<Smiler, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final Identifier TEXTURE = Backrooms.id("textures/entity/smiler.png");

    public SmilerRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.0F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }

    /** Fully bright, so its eyes and grin glow in the dark while its body stays black. */
    @Override
    public int getBlockLightLevel(Smiler smiler, BlockPos pos) {
        return 15;
    }

    @Override
    public void scale(HumanoidRenderState state, PoseStack poseStack) {
        poseStack.scale(0.85F, 1.32F, 0.85F);
    }
}
