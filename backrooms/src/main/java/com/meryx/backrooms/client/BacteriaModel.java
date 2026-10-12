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
 * Three blocks of black wire, after the found footage. A flat blob of a head with a curl of wire around it; wide
 * shoulders whose ends hook down; a spine like a kinked chain down to a bar of a pelvis; crooked legs. Its right arm
 * reaches out and down to the floor like a crutch, fingers splayed; its left bends at the elbow and reaches across its
 * body. Every part shakes constantly, and now and then the whole thing jerks.
 */
public class BacteriaModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Backrooms.id("bacteria"), "main");
    private static final float[] SPINE_BENDS = {0.18F, -0.34F, 0.32F, -0.2F};

    private final ModelPart pelvis;
    private final ModelPart[] spine = new ModelPart[SPINE_BENDS.length];
    private final ModelPart shoulders;
    private final ModelPart head;
    private final ModelPart curl;
    private final ModelPart crutchArm;
    private final ModelPart bentArm;
    private final ModelPart bentForearm;
    private final ModelPart rightLeg;
    private final ModelPart rightShin;
    private final ModelPart leftLeg;
    private final ModelPart leftShin;

    public BacteriaModel(ModelPart root) {
        super(root);
        this.pelvis = root.getChild("pelvis");
        ModelPart link = pelvis;
        for (int i = 0; i < spine.length; i++) {
            link = link.getChild("spine" + i);
            spine[i] = link;
        }
        this.shoulders = link.getChild("shoulders");
        this.head = shoulders.getChild("head");
        this.curl = head.getChild("curl");
        this.crutchArm = shoulders.getChild("right_arm");
        this.bentArm = shoulders.getChild("left_arm");
        this.bentForearm = bentArm.getChild("forearm");
        this.rightLeg = pelvis.getChild("right_leg");
        this.rightShin = rightLeg.getChild("shin");
        this.leftLeg = pelvis.getChild("left_leg");
        this.leftShin = leftLeg.getChild("shin");
    }

    private static CubeListBuilder cubes() {
        return CubeListBuilder.create().texOffs(0, 0);
    }

    /** A one-pixel rod hanging {@code length} down from its pivot, with a knot at the end. */
    private static CubeListBuilder hanging(float length) {
        return cubes().addBox(-0.5F, 0.0F, -0.5F, 1.0F, length, 1.0F)
                .addBox(-1.0F, length - 1.5F, -1.0F, 2.0F, 2.0F, 2.0F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Pelvis: a short bar 17.5 pixels up, knotted at the hips
        PartDefinition pelvis = root.addOrReplaceChild("pelvis", cubes()
                        .addBox(-3.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F)
                        .addBox(-4.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                        .addBox(2.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 6.5F, 0.0F));

        // Crooked legs: the right splays out then kinks back in; the left has a hook of wire at the knee
        PartDefinition rightLeg = pelvis.addOrReplaceChild("right_leg", hanging(9.0F), PartPose.offset(-3.0F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("shin", cubes()
                .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F)
                .addBox(-0.5F, 8.0F, -2.5F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.0F, 8.5F, 0.0F));
        PartDefinition leftLeg = pelvis.addOrReplaceChild("left_leg", hanging(9.0F)
                .addBox(0.5F, 7.0F, -0.5F, 2.5F, 1.0F, 1.0F)
                .addBox(2.0F, 5.5F, -0.5F, 1.0F, 1.5F, 1.0F), PartPose.offset(3.0F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("shin", cubes()
                .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F)
                .addBox(-0.5F, 8.0F, -2.5F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.0F, 8.5F, 0.0F));

        // The spine: four kinked links of a chain, each knotted where it meets the next
        PartDefinition link = pelvis;
        for (int i = 0; i < SPINE_BENDS.length; i++) {
            link = link.addOrReplaceChild("spine" + i, cubes()
                            .addBox(-0.5F, -5.5F, -0.5F, 1.0F, 5.5F, 1.0F)
                            .addBox(-1.0F, -6.5F, -1.0F, 2.0F, 2.0F, 2.0F)
                            .addBox(i % 2 == 0 ? 0.5F : -1.5F, -3.5F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, i == 0 ? 0.0F : -5.5F, 0.0F, 0.0F, 0.0F, SPINE_BENDS[i]));
        }

        // Shoulders: a wide bar whose ends hook down
        PartDefinition shoulders = link.addOrReplaceChild("shoulders", cubes()
                        .addBox(-8.5F, -0.5F, -0.5F, 17.0F, 1.0F, 1.0F)
                        .addBox(-9.5F, -0.5F, -0.5F, 1.0F, 3.0F, 1.0F)
                        .addBox(-9.0F, 2.5F, -0.5F, 1.5F, 1.0F, 1.0F)
                        .addBox(8.5F, -0.5F, -0.5F, 1.0F, 3.0F, 1.0F)
                        .addBox(7.5F, 2.5F, -0.5F, 1.5F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, 0.0F, 0.0F, 0.04F));

        // Head: a wide, flat, rounded blob on a short neck, with a loop of wire curling round it
        PartDefinition head = shoulders.addOrReplaceChild("head", cubes()
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .addBox(-3.0F, -3.0F, -2.5F, 6.0F, 1.0F, 5.0F)
                        .addBox(-4.0F, -5.0F, -3.0F, 8.0F, 2.0F, 6.0F)
                        .addBox(-3.0F, -6.0F, -2.5F, 6.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, -0.5F, 0.0F));
        head.addOrReplaceChild("curl", cubes()
                        .addBox(-5.0F, 0.0F, -3.5F, 9.0F, 1.0F, 1.0F)
                        .addBox(-5.0F, 0.0F, 2.5F, 7.0F, 1.0F, 1.0F)
                        .addBox(-5.0F, 0.0F, -3.5F, 1.0F, 1.0F, 7.0F)
                        .addBox(3.0F, 0.0F, -3.5F, 1.0F, 1.0F, 3.0F)
                        .addBox(3.0F, 1.0F, -1.5F, 1.0F, 1.5F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, 0.0F, 0.0F, -0.22F));

        // Right arm: one long, straight rod reaching out and down to the floor like a crutch, fingers splayed
        shoulders.addOrReplaceChild("right_arm", cubes()
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 38.5F, 1.0F)
                        .addBox(-1.0F, 13.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                        .addBox(-2.5F, 37.5F, -0.5F, 5.0F, 1.0F, 1.0F)
                        .addBox(-2.5F, 38.5F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .addBox(1.5F, 38.5F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .addBox(-0.5F, 38.5F, -2.5F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-8.5F, 0.5F, 0.0F, 0.0F, 0.0F, 0.33F));

        // Left arm: hangs to the elbow, then the forearm reaches across in front of its body
        PartDefinition leftArm = shoulders.addOrReplaceChild("left_arm", hanging(17.0F),
                PartPose.offsetAndRotation(8.5F, 0.5F, 0.0F, 0.0F, 0.0F, -0.06F));
        leftArm.addOrReplaceChild("forearm", cubes()
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 11.0F, 1.0F)
                        .addBox(-1.5F, 10.5F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .addBox(0.5F, 10.5F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 16.5F, -1.0F, 0.0F, 0.0F, 1.45F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float walk = state.walkAnimationPos * 0.45F;
        float swing = Math.min(state.walkAnimationSpeed, 1.0F);
        // It never stops shaking, and every so often it jerks hard
        float jerk = Mth.sin(age * 0.9F) * Mth.sin(age * 0.37F) > 0.8F ? Mth.sin(age * 9.0F) : 0.0F;

        pelvis.zRot = shake(age, 0.0F) + Mth.sin(walk) * 0.06F * swing;
        for (int i = 0; i < spine.length; i++) {
            spine[i].zRot = SPINE_BENDS[i] + shake(age, i + 1.0F) + (i == 2 ? jerk * 0.2F : 0.0F);
        }
        spine[0].xRot = 0.08F + swing * 0.12F;
        shoulders.zRot = 0.04F + shake(age, 5.0F) + jerk * 0.25F;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        head.zRot = shake(age, 6.0F) * 2.0F + jerk * 0.5F;
        curl.zRot = -0.22F + shake(age, 7.0F) * 3.0F;

        // The crutch arm plants and swings like a third leg; the bent arm twitches
        crutchArm.xRot = Mth.cos(walk + Mth.PI) * 0.35F * swing;
        crutchArm.zRot = 0.33F + shake(age, 8.0F);
        bentArm.xRot = Mth.cos(walk) * 0.25F * swing + jerk * 0.3F;
        bentArm.zRot = -0.06F + shake(age, 9.0F);
        bentForearm.zRot = 1.45F + shake(age, 10.0F) * 2.0F - jerk * 0.4F;

        rightLeg.xRot = Mth.cos(walk) * 0.7F * swing - 0.1F;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 0.7F * swing - 0.1F;
        rightLeg.zRot = 0.16F + shake(age, 11.0F);
        leftLeg.zRot = -0.04F + shake(age, 12.0F);
        rightShin.zRot = -0.28F;
        leftShin.zRot = 0.08F;
        rightShin.xRot = 0.2F + Math.max(0.0F, Mth.sin(walk)) * 0.6F * swing;
        leftShin.xRot = 0.2F + Math.max(0.0F, -Mth.sin(walk)) * 0.6F * swing;
    }

    /** A small, fast, irregular tremble, different for each part. */
    private static float shake(float age, float seed) {
        return (Mth.sin(age * 2.7F + seed * 1.3F) * 0.6F + Mth.sin(age * 7.1F + seed * 2.9F) * 0.4F) * 0.035F;
    }
}
