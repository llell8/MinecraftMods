package com.hackclient.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Turns world positions into screen positions for the current frame, so ESP boxes and tracers can be
 * drawn on top of the game (visible through walls). Uses the player's eyes and view rotation (first
 * person), and the real FOV captured in GameRendererMixin (so sprinting and Zoom are accounted for).
 */
public final class Projection {
	private static final double NEAR = 0.05;
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

	/** World point in camera space: {right, up, forward}. */
	public double[] camera(double wx, double wy, double wz) {
		double dx = wx - eye.x, dy = wy - eye.y, dz = wz - eye.z;
		return new double[] {dx * rx + dz * rz, dx * ux + dy * uy + dz * uz, dx * fx + dy * fy + dz * fz};
	}

	/** Camera-space point (must be in front) to screen {x, y}. */
	public double[] screen(double[] c) {
		return new double[] {
				width / 2 + (c[0] / c[2]) / (tanHalf * aspect) * (width / 2),
				height / 2 - (c[1] / c[2]) / tanHalf * (height / 2)
		};
	}

	/** @return screen {x, y}, or null if the point is behind the camera */
	public double[] project(double wx, double wy, double wz) {
		double[] c = camera(wx, wy, wz);
		return c[2] < NEAR ? null : screen(c);
	}

	/**
	 * Where a tracer should end: the projected point if it's in front of you, otherwise a point off the
	 * screen edge in the direction you'd have to turn.
	 */
	public double[] towards(double wx, double wy, double wz) {
		double[] c = camera(wx, wy, wz);
		if (c[2] >= NEAR) return screen(c);
		double len = Math.max(1e-6, Math.hypot(c[0], c[1]));
		double reach = Math.hypot(width, height);
		return new double[] {width / 2 + c[0] / len * reach, height / 2 - c[1] / len * reach};
	}

	// ---- Boxes ----

	// Corner i of a box: x from bit 0, y from bit 1, z from bit 2
	private static final int[][] FACES = {
			{0, 1, 5, 4}, {2, 3, 7, 6}, // bottom, top
			{0, 1, 3, 2}, {4, 5, 7, 6}, // north, south
			{0, 2, 6, 4}, {1, 3, 7, 5}  // west, east
	};

	/** Draws a 3D box: translucent faces and/or outline edges, clipped against the camera. */
	public void box(QuadBatch batch, AABB box, ShapeMode mode, int sideColor, int lineColor, double lineWidth) {
		double[][] cam = new double[8][];
		boolean allBehind = true;
		boolean anyBehind = false;
		for (int i = 0; i < 8; i++) {
			cam[i] = camera((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ);
			if (cam[i][2] < NEAR) anyBehind = true;
			else allBehind = false;
		}
		if (allBehind) return;

		if (mode.sides() && !anyBehind) {
			double[][] s = new double[8][];
			for (int i = 0; i < 8; i++) s[i] = screen(cam[i]);
			for (int[] f : FACES) {
				batch.quad(s[f[0]][0], s[f[0]][1], s[f[1]][0], s[f[1]][1], s[f[2]][0], s[f[2]][1], s[f[3]][0], s[f[3]][1], sideColor);
			}
		}
		if (mode.lines()) {
			for (int i = 0; i < 8; i++) {
				for (int bit = 1; bit <= 4; bit <<= 1) {
					if ((i & bit) == 0) edge(batch, cam[i], cam[i | bit], lineColor, lineWidth);
				}
			}
		}
	}

	/** A line between two camera-space points, cut where it passes behind the camera. */
	private void edge(QuadBatch batch, double[] a, double[] b, int color, double width) {
		if (a[2] < NEAR && b[2] < NEAR) return;
		if (a[2] < NEAR) a = clip(b, a);
		else if (b[2] < NEAR) b = clip(a, b);
		double[] sa = screen(a), sb = screen(b);
		batch.line(sa[0], sa[1], sb[0], sb[1], width, color);
	}

	private static double[] clip(double[] front, double[] behind) {
		double t = (front[2] - NEAR) / (front[2] - behind[2]);
		return new double[] {front[0] + (behind[0] - front[0]) * t, front[1] + (behind[1] - front[1]) * t, NEAR};
	}

	/** Screen-space rectangle around a box (for 2D mode), or null if any corner is behind you. */
	public double[] rect(AABB box) {
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			double[] s = project((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ);
			if (s == null) return null;
			minX = Math.min(minX, s[0]);
			minY = Math.min(minY, s[1]);
			maxX = Math.max(maxX, s[0]);
			maxY = Math.max(maxY, s[1]);
		}
		return new double[] {minX, minY, maxX, maxY};
	}
}
