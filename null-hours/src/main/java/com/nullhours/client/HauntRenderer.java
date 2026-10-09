package com.nullhours.client;

import com.nullhours.entity.HauntEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Draws a haunt entity as a plain humanoid with its own skin. */
public class HauntRenderer<T extends HauntEntity> extends HumanoidMobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private final Identifier texture;
	private final boolean fullBright;

	public HauntRenderer(EntityRendererProvider.Context context, Identifier texture, boolean fullBright) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.0f);
		this.texture = texture;
		this.fullBright = fullBright;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return texture;
	}

	@Override
	protected int getBlockLightLevel(T entity, BlockPos pos) {
		// Fully lit so the eyes still show up in the pitch dark
		return fullBright ? 15 : super.getBlockLightLevel(entity, pos);
	}
}
