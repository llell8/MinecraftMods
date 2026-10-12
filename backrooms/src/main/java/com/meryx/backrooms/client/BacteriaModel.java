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
 * body. Every rod is wrapped in a thinner twisted wire and knotted with burls; loose strands hang from its shoulders and
 * head and sway. Every part shakes constantly, and now and then the whole thing jerks.
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
    private final ModelPart[] strands;

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
        this.strands = new ModelPart[] {shoulders.getChild("strand_right"), shoulders.getChild("strand_left"),
                shoulders.getChild("strand_back"), head.getChild("strand")};
    }

    private static CubeListBuilder cubes() {
        return CubeListBuilder.create().texOffs(0, 0);
    }

    /** A one-pixel rod hanging {@code length} down from its pivot, wrapped and with a knot at the end. */
    private static CubeListBuilder hanging(float length) {
        return wrap(cubes().addBox(-0.5F, 0.0F, -0.5F, 1.0F, length, 1.0F)
                .addBox(-1.0F, length - 1.5F, -1.0F, 2.0F, 2.0F, 2.0F), 0.5F, length - 2.0F);
    }

    /**
     * A thinner wire twisted round a vertical rod between {@code from} and {@code to}: little nubs that wind round all
     * four sides, with a burl every so often.
     */
    private static CubeListBuilder wrap(CubeListBuilder rod, float from, float to) {
        int step = 0;
        for (float y = from; y < to; y += 1.25F, step++) {
            switch (step % 4) {
                case 0 -> rod.addBox(0.4F, y, -0.3F, 0.4F, 1.1F, 0.6F);
                case 1 -> rod.addBox(-0.3F, y, 0.4F, 0.6F, 1.1F, 0.4F);
                case 2 -> rod.addBox(-0.8F, y, -0.3F, 0.4F, 1.1F, 0.6F);
                default -> rod.addBox(-0.3F, y, -0.8F, 0.6F, 1.1F, 0.4F);
            }
            if (step % 7 == 5) {
                rod.addBox(-0.75F, y, -0.75F, 1.5F, 1.5F, 1.5F);
            }
        }
        return rod;
    }

    /** A loose half-pixel strand hanging {@code length} down, with a kink halfway. */
    private static CubeListBuilder strand(float length) {
        float half = length / 2.0F;
        return cubes().addBox(-0.25F, 0.0F, -0.25F, 0.5F, half, 0.5F)
                .addBox(0.25F, half - 0.5F, -0.25F, 0.5F, 0.5F, 0.5F)
                .addBox(0.5F, half, -0.25F, 0.5F, length - half, 0.5F)
                .addBox(0.25F, length - 0.25F, -0.5F, 1.0F, 0.75F, 1.0F);
    }

    /** A wrapped shin ending in a long, thin, jointed foot that grips the floor. */
    private static CubeListBuilder foot() {
        return wrap(cubes()
                .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F)
                .addBox(-0.5F, 8.0F, -2.5F, 1.0F, 1.0F, 3.0F)
                .addBox(-0.4F, 8.4F, -3.6F, 0.8F, 0.6F, 1.2F)
                .addBox(-1.4F, 8.4F, -2.0F, 0.8F, 0.6F, 1.6F)
                .addBox(0.6F, 8.4F, -2.2F, 0.8F, 0.6F, 1.8F)
                .addBox(-0.4F, 8.3F, 0.5F, 0.8F, 0.7F, 1.0F), 0.5F, 7.5F);
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
        rightLeg.addOrReplaceChild("shin", foot(), PartPose.offset(0.0F, 8.5F, 0.0F));
        PartDefinition leftLeg = pelvis.addOrReplaceChild("left_leg", hanging(9.0F)
                .addBox(0.5F, 7.0F, -0.5F, 2.5F, 1.0F, 1.0F)
                .addBox(2.0F, 5.5F, -0.5F, 1.0F, 1.5F, 1.0F), PartPose.offset(3.0F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("shin", foot(), PartPose.offset(0.0F, 8.5F, 0.0F));

        // The spine: four kinked links of a chain, each knotted where it meets the next
        PartDefinition link = pelvis;
        for (int i = 0; i < SPINE_BENDS.length; i++) {
            float side = i % 2 == 0 ? 1.0F : -1.0F;
            link = link.addOrReplaceChild("spine" + i, wrap(cubes()
                            .addBox(-0.5F, -5.5F, -0.5F, 1.0F, 5.5F, 1.0F)
                            .addBox(-1.0F, -6.5F, -1.0F, 2.0F, 2.0F, 2.0F)
                            .addBox(side > 0 ? 0.5F : -1.5F, -3.5F, -0.5F, 1.0F, 1.0F, 1.0F)
                            // A rib: a short wire curling off to one side and back
                            .addBox(side > 0 ? 0.5F : -3.5F, -2.0F, -0.25F, 3.0F, 0.5F, 0.5F)
                            .addBox(side > 0 ? 3.0F : -3.5F, -2.0F, -0.25F, 0.5F, 1.5F, 0.5F)
                            .addBox(side > 0 ? -2.5F : 0.5F, -4.75F, -0.25F, 2.0F, 0.5F, 0.5F), -5.0F, -1.0F),
                    PartPose.offsetAndRotation(0.0F, i == 0 ? 0.0F : -5.5F, 0.0F, 0.0F, 0.0F, SPINE_BENDS[i]));
        }

        // Shoulders: a wide bar whose ends hook down
        PartDefinition shoulders = link.addOrReplaceChild("shoulders", cubes()
                        .addBox(-8.5F, -0.5F, -0.5F, 17.0F, 1.0F, 1.0F)
                        .addBox(-9.5F, -0.5F, -0.5F, 1.0F, 3.0F, 1.0F)
                        .addBox(-9.0F, 2.5F, -0.5F, 1.5F, 1.0F, 1.0F)
                        .addBox(8.5F, -0.5F, -0.5F, 1.0F, 3.0F, 1.0F)
                        .addBox(7.5F, 2.5F, -0.5F, 1.5F, 1.0F, 1.0F)
                        // Knots along the bar, and a second, thinner bar twisted round it
                        .addBox(-6.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                        .addBox(4.5F, -1.0F, -1.0F, 1.5F, 1.5F, 1.5F)
                        .addBox(-1.0F, -0.75F, -1.0F, 2.0F, 1.5F, 2.0F)
                        .addBox(-7.5F, 0.4F, -0.3F, 5.0F, 0.4F, 0.6F)
                        .addBox(-2.5F, -0.8F, -0.3F, 5.0F, 0.4F, 0.6F)
                        .addBox(2.5F, 0.4F, -0.3F, 5.0F, 0.4F, 0.6F),
                PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, 0.0F, 0.0F, 0.04F));
        shoulders.addOrReplaceChild("strand_right", strand(7.0F), PartPose.offset(-6.0F, 0.5F, 0.3F));
        shoulders.addOrReplaceChild("strand_left", strand(5.0F), PartPose.offset(5.0F, 0.5F, -0.3F));
        shoulders.addOrReplaceChild("strand_back", strand(9.0F), PartPose.offset(1.5F, 0.5F, 0.8F));

        // Head: a wide, flat, rounded blob on a short neck, with a loop of wire curling round it
        PartDefinition head = shoulders.addOrReplaceChild("head", cubes()
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .addBox(-3.0F, -3.0F, -2.5F, 6.0F, 1.0F, 5.0F)
                        .addBox(-4.0F, -5.0F, -3.0F, 8.0F, 2.0F, 6.0F)
                        .addBox(-3.0F, -6.0F, -2.5F, 6.0F, 1.0F, 5.0F)
                        // Lumps on the blob
                        .addBox(-4.5F, -4.5F, -1.5F, 1.0F, 1.5F, 2.5F)
                        .addBox(3.5F, -5.0F, 0.0F, 1.0F, 1.5F, 2.0F)
                        .addBox(-1.5F, -6.75F, -1.0F, 2.5F, 0.75F, 2.0F)
                        .addBox(1.0F, -5.5F, -3.5F, 2.0F, 2.0F, 1.0F)
                        .addBox(-2.5F, -3.5F, 2.5F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -0.5F, 0.0F));
        head.addOrReplaceChild("strand", strand(6.0F), PartPose.offset(-2.0F, -3.0F, -1.5F));
        head.addOrReplaceChild("curl", cubes()
                        .addBox(-5.0F, 0.0F, -3.5F, 9.0F, 1.0F, 1.0F)
                        .addBox(-5.0F, 0.0F, 2.5F, 7.0F, 1.0F, 1.0F)
                        .addBox(-5.0F, 0.0F, -3.5F, 1.0F, 1.0F, 7.0F)
                        .addBox(3.0F, 0.0F, -3.5F, 1.0F, 1.0F, 3.0F)
                        .addBox(3.0F, 1.0F, -1.5F, 1.0F, 1.5F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, 0.0F, 0.0F, -0.22F));

        // Right arm: one long, straight rod reaching out and down to the floor like a crutch, fingers splayed
        shoulders.addOrReplaceChild("right_arm", wrap(cubes()
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 38.5F, 1.0F)
                        .addBox(-1.0F, 13.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                        .addBox(-0.9F, 27.0F, -0.9F, 1.8F, 1.8F, 1.8F)
                        // A palm and four jointed fingers splayed on the floor
                        .addBox(-2.5F, 37.5F, -0.5F, 5.0F, 1.0F, 1.0F)
                        .addBox(-3.0F, 38.0F, -0.4F, 0.8F, 1.2F, 0.8F)
                        .addBox(-3.6F, 39.0F, -0.4F, 0.8F, 1.5F, 0.8F)
                        .addBox(-1.4F, 38.5F, -0.4F, 0.8F, 2.0F, 0.8F)
                        .addBox(1.0F, 38.5F, -0.4F, 0.8F, 1.0F, 0.8F)
                        .addBox(1.6F, 39.3F, -0.4F, 0.8F, 1.2F, 0.8F)
                        .addBox(2.2F, 38.0F, -0.4F, 0.8F, 0.8F, 0.8F)
                        .addBox(2.8F, 38.6F, -0.4F, 0.8F, 1.9F, 0.8F)
                        .addBox(-0.4F, 38.5F, -2.0F, 0.8F, 0.8F, 1.6F)
                        .addBox(-0.4F, 39.2F, -2.8F, 0.8F, 1.3F, 0.8F), 1.5F, 36.0F),
                PartPose.offsetAndRotation(-8.5F, 0.5F, 0.0F, 0.0F, 0.0F, 0.33F));

        // Left arm: hangs to the elbow, then the forearm reaches across in front of its body
        PartDefinition leftArm = shoulders.addOrReplaceChild("left_arm", hanging(17.0F),
                PartPose.offsetAndRotation(8.5F, 0.5F, 0.0F, 0.0F, 0.0F, -0.06F));
        leftArm.addOrReplaceChild("forearm", wrap(cubes()
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 11.0F, 1.0F)
                        // Long, jointed fingers, curled
                        .addBox(-1.6F, 10.5F, -0.4F, 0.8F, 2.0F, 0.8F)
                        .addBox(-2.2F, 12.3F, -0.4F, 0.8F, 1.5F, 0.8F)
                        .addBox(0.8F, 10.5F, -0.4F, 0.8F, 2.5F, 0.8F)
                        .addBox(1.4F, 12.8F, -0.4F, 0.8F, 1.2F, 0.8F)
                        .addBox(-0.4F, 10.8F, 0.4F, 0.8F, 2.8F, 0.8F), 0.5F, 9.5F),
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

        // Loose strands sway and swing behind as it moves
        for (int i = 0; i < strands.length; i++) {
            strands[i].zRot = Mth.sin(age * 0.11F + i * 1.7F) * 0.18F + shake(age, 13.0F + i) * 3.0F;
            strands[i].xRot = 0.15F + swing * 0.5F + Mth.sin(age * 0.07F + i) * 0.1F;
        }

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
