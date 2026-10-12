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
 * Three blocks of black wire: legs, a crooked spine and arms all as thin as a pixel, knotted at the joints. On top, a
 * crossbar of shoulders with curling tendrils at each end, and above that a dark blob of a head with a ring around it
 * like a hat brim. The arms reach all the way down to the floor. Every part shakes constantly, and now and then the
 * whole thing jerks.
 */
public class BacteriaModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Backrooms.id("bacteria"), "main");

    private final ModelPart spine;
    private final ModelPart upperSpine;
    private final ModelPart shoulders;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart rightLeg;
    private final ModelPart rightShin;
    private final ModelPart leftLeg;
    private final ModelPart leftShin;

    public BacteriaModel(ModelPart root) {
        super(root);
        this.spine = root.getChild("spine");
        this.upperSpine = spine.getChild("upper_spine");
        this.shoulders = upperSpine.getChild("shoulders");
        this.head = shoulders.getChild("head");
        this.rightArm = shoulders.getChild("right_arm");
        this.rightForearm = rightArm.getChild("forearm");
        this.leftArm = shoulders.getChild("left_arm");
        this.leftForearm = leftArm.getChild("forearm");
        this.rightLeg = root.getChild("right_leg");
        this.rightShin = rightLeg.getChild("shin");
        this.leftLeg = root.getChild("left_leg");
        this.leftShin = leftLeg.getChild("shin");
    }

    /** A one-pixel rod {@code length} long hanging down (positive) or standing up (negative) from its pivot. */
    private static CubeListBuilder rod(float length) {
        return length > 0
                ? CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, length, 1.0F)
                : CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, length, -0.5F, 1.0F, -length, 1.0F);
    }

    /** A rod with a knot at its far end. */
    private static CubeListBuilder knottedRod(float length) {
        CubeListBuilder rod = rod(length);
        float end = length > 0 ? length - 1.0F : length;
        return rod.texOffs(0, 0).addBox(-1.0F, end - 0.5F, -1.0F, 2.0F, 2.0F, 2.0F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Hips 18 pixels up. Legs bend at the knee.
        for (String side : new String[] {"right", "left"}) {
            float x = side.equals("right") ? -1.5F : 1.5F;
            PartDefinition leg = root.addOrReplaceChild(side + "_leg", knottedRod(9.5F), PartPose.offset(x, 6.0F, 0.0F));
            leg.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F)
                    .addBox(-0.5F, 8.0F, -2.5F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.0F, 9.0F, 0.0F));
        }

        // A crooked spine in two pieces, up to the shoulders 22 pixels above the hips
        PartDefinition spine = root.addOrReplaceChild("spine", knottedRod(-11.0F).texOffs(0, 0).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        PartDefinition upperSpine = spine.addOrReplaceChild("upper_spine", knottedRod(-11.0F), PartPose.offset(0.0F, -11.0F, 0.0F));

        // Shoulders: a crossbar with tendrils curling up at each end
        PartDefinition shoulders = upperSpine.addOrReplaceChild("shoulders", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-8.0F, -0.5F, -0.5F, 16.0F, 1.0F, 1.0F)
                        .addBox(-9.0F, -2.5F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .addBox(-8.0F, -3.5F, -0.5F, 2.0F, 1.0F, 1.0F)
                        .addBox(8.0F, -2.5F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .addBox(6.0F, -3.5F, -0.5F, 2.0F, 1.0F, 1.0F)
                        .addBox(-4.0F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F)
                        .addBox(3.0F, -1.5F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -11.0F, 0.0F));

        // Head: a dark blob on a short neck, with a ring around it like a hat brim
        shoulders.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .addBox(-4.0F, -3.0F, -4.0F, 8.0F, 1.0F, 8.0F)
                        .addBox(-2.5F, -6.0F, -2.5F, 5.0F, 3.0F, 5.0F)
                        .addBox(-1.5F, -7.0F, -1.5F, 3.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, -0.5F, 0.0F));

        // Arms from the ends of the crossbar all the way down to the floor, with clawed hands
        for (String side : new String[] {"right", "left"}) {
            float x = side.equals("right") ? -7.5F : 7.5F;
            PartDefinition arm = shoulders.addOrReplaceChild(side + "_arm", knottedRod(20.0F), PartPose.offset(x, 0.0F, 0.0F));
            arm.addOrReplaceChild("forearm", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 18.0F, 1.0F)
                    .addBox(-2.0F, 17.0F, -0.5F, 4.0F, 1.0F, 1.0F)
                    .addBox(-2.0F, 18.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                    .addBox(1.0F, 18.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                    .addBox(-0.5F, 18.0F, -1.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 19.0F, 0.0F));
        }
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

        spine.zRot = 0.12F + shake(age, 1.0F) + jerk * 0.15F;
        spine.xRot = 0.1F + swing * 0.15F;
        upperSpine.zRot = -0.22F + shake(age, 2.0F);
        shoulders.zRot = 0.1F + shake(age, 3.0F) + jerk * 0.3F;

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        head.zRot = shake(age, 4.0F) * 2.0F + jerk * 0.5F;

        // Arms splay out a little and swing loosely, bending at the elbow; they drag along when it moves
        rightArm.zRot = 0.14F + shake(age, 5.0F);
        leftArm.zRot = -0.14F + shake(age, 6.0F);
        rightArm.xRot = Mth.cos(walk + Mth.PI) * 0.5F * swing - 0.05F;
        leftArm.xRot = Mth.cos(walk) * 0.5F * swing - 0.05F;
        rightForearm.zRot = -0.2F + shake(age, 7.0F);
        leftForearm.zRot = 0.2F + shake(age, 8.0F);
        rightForearm.xRot = 0.1F + jerk * 0.4F;
        leftForearm.xRot = 0.1F - jerk * 0.4F;

        // Knees bend backwards a touch; long, stalking strides
        rightLeg.xRot = Mth.cos(walk) * 0.7F * swing - 0.12F;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 0.7F * swing - 0.12F;
        rightShin.xRot = 0.25F + Math.max(0.0F, Mth.sin(walk)) * 0.6F * swing;
        leftShin.xRot = 0.25F + Math.max(0.0F, -Mth.sin(walk)) * 0.6F * swing;
        rightLeg.zRot = 0.05F + shake(age, 9.0F);
        leftLeg.zRot = -0.05F + shake(age, 10.0F);
    }

    /** A small, fast, irregular tremble, different for each part. */
    private static float shake(float age, float seed) {
        return (Mth.sin(age * 2.7F + seed * 1.3F) * 0.6F + Mth.sin(age * 7.1F + seed * 2.9F) * 0.4F) * 0.035F;
    }
}
