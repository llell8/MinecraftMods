package com.meryx.backrooms.client;

import com.meryx.backrooms.Backrooms;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Nothing but a face: two glowing eyes and a grin full of teeth hanging in the dark, about two blocks up. Everything
 * else in the texture is transparent. The face is drawn at 8x detail and {@link SmilerRenderer} scales it down.
 */
public class SmilerModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Backrooms.id("smiler"), "main");
    /** The renderer's scale, which also scales this model's distance from the ground. */
    public static final float SCALE = 0.125F;
    private static final float FACE_Y = 24.0F - 2.1F * 16.0F / SCALE;

    private final ModelPart face;

    public SmilerModel(ModelPart root) {
        super(root);
        this.face = root.getChild("face");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("face",
                CubeListBuilder.create().texOffs(0, 0).addBox(-64.0F, -40.0F, -0.5F, 128.0F, 80.0F, 1.0F),
                PartPose.offset(0.0F, FACE_Y, 0.0F));
        return LayerDefinition.create(mesh, 512, 128);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        // Always turned towards whatever it is watching, drifting gently up and down
        face.yRot = state.yRot * Mth.DEG_TO_RAD;
        face.xRot = state.xRot * Mth.DEG_TO_RAD;
        face.y = FACE_Y + Mth.sin(state.ageInTicks * 0.08F) * 8.0F;
    }
}
