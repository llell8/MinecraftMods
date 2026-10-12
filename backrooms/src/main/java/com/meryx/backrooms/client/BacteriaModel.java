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
 * About three blocks of black, starved-looking limbs: long legs, a body hunched forward from the hips, arms that hang
 * nearly to the floor and a small stretched head. It twitches: every so often its head and arms jerk.
 */
public class BacteriaModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Backrooms.id("bacteria"), "main");
    private static final float HUNCH = 0.55F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public BacteriaModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Legs: 28 long, so the hips sit 1.75 blocks up
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 28.0F, 2.0F),
                PartPose.offset(-2.5F, -4.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 28.0F, 2.0F),
                PartPose.offset(2.5F, -4.0F, 0.0F));
        // The body grows up from the hips and leans forward; head and arms hang off its top
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(8, 0).addBox(-3.5F, -18.0F, -2.0F, 7.0F, 18.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, HUNCH, 0.0F, 0.0F));
        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(30, 0).addBox(-2.5F, -10.0F, -3.5F, 5.0F, 10.0F, 6.0F),
                PartPose.offset(0.0F, -18.0F, -0.5F));
        body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 30).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 32.0F, 2.0F)
                        .texOffs(8, 30).addBox(-1.5F, 31.0F, -1.5F, 3.0F, 4.0F, 1.0F),
                PartPose.offset(-4.5F, -16.0F, 0.0F));
        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 30).mirror().addBox(-1.0F, -1.0F, -1.0F, 2.0F, 32.0F, 2.0F)
                        .texOffs(8, 30).addBox(-1.5F, 31.0F, -1.5F, 3.0F, 4.0F, 1.0F),
                PartPose.offset(4.5F, -16.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float walk = state.walkAnimationPos * 0.5F;
        float swing = Math.min(state.walkAnimationSpeed, 1.0F);
        // A twitch: a short, violent jerk every couple of seconds
        float twitch = Mth.sin(age * 0.9F) * Mth.sin(age * 0.37F) > 0.8F ? Mth.sin(age * 9.0F) : 0.0F;

        // The body is hunched, so tilt the head back up to look ahead
        head.xRot = state.xRot * Mth.DEG_TO_RAD - HUNCH;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.zRot = twitch * 0.6F;

        // Arms hang forward, cancelling out the hunch, and swing loosely
        rightArm.xRot = -HUNCH + 0.15F + Mth.cos(walk + Mth.PI) * 0.7F * swing + twitch * 0.3F;
        leftArm.xRot = -HUNCH + 0.15F + Mth.cos(walk) * 0.7F * swing - twitch * 0.3F;
        rightArm.zRot = 0.08F + Mth.sin(age * 0.06F) * 0.05F;
        leftArm.zRot = -0.08F - Mth.sin(age * 0.06F) * 0.05F;
        body.xRot = HUNCH + Mth.sin(age * 0.1F) * 0.03F;

        rightLeg.xRot = Mth.cos(walk) * 0.8F * swing;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 0.8F * swing;
    }
}
