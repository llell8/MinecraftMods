package com.hackclient.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Turns world positions into screen positions for the current frame, so ESP boxes and tracers can be
 * drawn as a 2D overlay. Uses the player's eyes and view rotation (first person), and the real FOV
 * captured in GameRendererMixin (so sprinting and Zoom are accounted for).
 */
public final class Projection {
	private static double fov = 70;

	private final Vec3 eye;
	private final double fx, fy, fz; // forward
	private final double rx, rz;     // right (always horizontal)
	private final double ux, uy, uz; // up
	private final double tanHalf;
	private final double aspect;
	public final double width;
	public final double height;

	/** Called from GameRendererMixin with the FOV the world is drawn with. */
	public static void setFov(double value) {
		fov = value;
	}

	public Projection(float partialTick, double width, double height) {
		Minecraft mc = Minecraft.getInstance();
		this.eye = mc.player.getEyePosition(partialTick);
		double yaw = Math.toRadians(mc.player.getViewYRot(partialTick));
		double pitch = Math.toRadians(mc.player.getViewXRot(partialTick));

		fx = -Math.sin(yaw) * Math.cos(pitch);
		fy = -Math.sin(pitch);
		fz = Math.cos(yaw) * Math.cos(pitch);
		rx = -Math.cos(yaw);
		rz = -Math.sin(yaw);
		// up = right x forward
		ux = -rz * fy;
		uy = rz * fx - rx * fz;
		uz = rx * fy;

		this.width = width;
		this.height = height;
		this.aspect = width / height;
		this.tanHalf = Math.tan(Math.toRadians(fov) / 2);
	}

	/** @return {x, y, depth} in screen pixels, or null if the point is behind the camera */
	public double[] project(double wx, double wy, double wz) {
		double dx = wx - eye.x, dy = wy - eye.y, dz = wz - eye.z;
		double z = dx * fx + dy * fy + dz * fz;
		if (z < 0.05) return null;
		double x = dx * rx + dz * rz;
		double y = dx * ux + dy * uy + dz * uz;
		return new double[] {
				width / 2 + (x / z) / (tanHalf * aspect) * (width / 2),
				height / 2 - (y / z) / tanHalf * (height / 2),
				z
		};
	}

	/**
	 * Where a tracer should end: the projected point if it's in front of you, otherwise a point on the
	 * screen edge in the direction you'd have to turn.
	 */
	public double[] towards(double wx, double wy, double wz) {
		double[] p = project(wx, wy, wz);
		if (p != null) return p;
		double dx = wx - eye.x, dy = wy - eye.y, dz = wz - eye.z;
		double x = dx * rx + dz * rz;
		double y = dx * ux + dy * uy + dz * uz;
		double len = Math.max(1e-6, Math.hypot(x, y));
		double reach = Math.hypot(width, height);
		return new double[] {width / 2 + x / len * reach, height / 2 - y / len * reach, 0};
	}
}
