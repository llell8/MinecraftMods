package com.nullhours.client;

import com.nullhours.NullHours;
import com.nullhours.entity.HauntEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/** Draws a haunt entity as a flat picture that always turns to face the camera. */
public class SpriteRenderer<T extends HauntEntity> extends LivingEntityRenderer<T, SpriteRenderState, SpriteModel> {
	private final Identifier[] frames;
	private final boolean fullBright;

	public SpriteRenderer(EntityRendererProvider.Context context, String name, boolean fullBright) {
		super(context, new SpriteModel(SpriteModel.create()), 0.0f);
		this.frames = new Identifier[] {
				NullHours.id("textures/entity/" + name + "_0.png"),
				NullHours.id("textures/entity/" + name + "_1.png")
		};
		this.fullBright = fullBright;
	}

	@Override
	public SpriteRenderState createRenderState() {
		return new SpriteRenderState();
	}

	@Override
	public void extractRenderState(T entity, SpriteRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		Entity viewer = Minecraft.getInstance().getCameraEntity();
		if (viewer != null) {
			double dx = viewer.getX() - entity.getX();
			double dz = viewer.getZ() - entity.getZ();
			state.bodyRot = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
		}
		boolean walking = state.walkAnimationSpeed > 0.1f;
		state.frame = walking ? (int) (state.walkAnimationPos * 0.6f) & 1 : 0;
	}

	@Override
	public Identifier getTextureLocation(SpriteRenderState state) {
		return frames[state.frame];
	}

	@Override
	protected int getBlockLightLevel(T entity, BlockPos pos) {
		// Fully lit so it still shows up in the pitch dark
		return fullBright ? 15 : super.getBlockLightLevel(entity, pos);
	}
}
