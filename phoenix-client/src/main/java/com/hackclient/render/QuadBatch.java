package com.hackclient.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Lots of coloured quads (box faces and line segments) drawn as one GUI element, so hundreds of ESP boxes
 * cost a single draw call. Unlike GuiGraphics.fill, the quads can have any shape and angle.
 */
public final class QuadBatch implements GuiElementRenderState {
	private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath("hackclient", "esp_quads"))
			.withUsePipelineDrawModeForGui(true)
			.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
			.withCull(false)
			.build();

	private final Matrix3x2f matrix;
	private final ScreenRectangle bounds;
	private float[] points = new float[8 * 256];
	private int[] colors = new int[256];
	private int count;

	public QuadBatch(GuiGraphics graphics, int width, int height) {
		this.matrix = new Matrix3x2f(graphics.pose());
		this.bounds = new ScreenRectangle(0, 0, width, height).transformMaxBounds(matrix);
	}

	public boolean isEmpty() {
		return count == 0;
	}

	public void quad(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, int color) {
		if (count == colors.length) {
			points = java.util.Arrays.copyOf(points, points.length * 2);
			colors = java.util.Arrays.copyOf(colors, colors.length * 2);
		}
		int i = count * 8;
		points[i] = (float) x0;
		points[i + 1] = (float) y0;
		points[i + 2] = (float) x1;
		points[i + 3] = (float) y1;
		points[i + 4] = (float) x2;
		points[i + 5] = (float) y2;
		points[i + 6] = (float) x3;
		points[i + 7] = (float) y3;
		colors[count++] = color;
	}

	/** A straight line of the given width, at any angle. */
	public void line(double x0, double y0, double x1, double y1, double width, int color) {
		double dx = x1 - x0, dy = y1 - y0;
		double len = Math.hypot(dx, dy);
		if (len < 1e-3) return;
		double nx = -dy / len * width / 2, ny = dx / len * width / 2;
		quad(x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny, color);
	}

	public void submit(GuiGraphics graphics) {
		if (count > 0) graphics.guiRenderState.submitGuiElement(this);
	}

	@Override
	public void buildVertices(VertexConsumer vertices) {
		for (int q = 0; q < count; q++) {
			int i = q * 8;
			int color = colors[q];
			for (int v = 0; v < 4; v++) {
				vertices.addVertexWith2DPose(matrix, points[i + v * 2], points[i + v * 2 + 1]).setColor(color);
			}
		}
	}

	@Override
	public RenderPipeline pipeline() {
		return PIPELINE;
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.noTexture();
	}

	@Override
	public @Nullable ScreenRectangle scissorArea() {
		return null;
	}

	@Override
	public @Nullable ScreenRectangle bounds() {
		return bounds;
	}
}
