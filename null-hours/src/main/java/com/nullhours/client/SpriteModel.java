package com.nullhours.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * A single flat panel, one block wide and two blocks tall, that shows a picture of the
 * entity. The renderer keeps it turned towards the camera so it looks like a 2D cutout.
 */
public class SpriteModel extends EntityModel<SpriteRenderState> {
	public SpriteModel(ModelPart root) {
		super(root);
	}

	/** The texture is laid out as 32x32 units: front picture on the left half, back on the right. */
	public static ModelPart create() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("sprite",
				CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -8.0f, 0.0f, 16.0f, 32.0f, 0.0f),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 32).bakeRoot();
	}
}
