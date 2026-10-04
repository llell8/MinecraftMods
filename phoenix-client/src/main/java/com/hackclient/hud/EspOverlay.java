package com.hackclient.hud;

import com.hackclient.HackClient;
import com.hackclient.module.modules.render.BlockESP;
import com.hackclient.module.modules.render.Tracers;
import com.hackclient.render.Draw;
import com.hackclient.render.Projection;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Draws BlockESP boxes and Tracers lines on top of the game. Works in real screen pixels
 * (undoing the GUI scale) so lines stay thin.
 */
public class EspOverlay implements HudElement {
	@Override
	public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.options.hideGui) return;
		BlockESP blockEsp = HackClient.getModuleManager().getIfEnabled(BlockESP.class);
		Tracers tracers = HackClient.getModuleManager().getIfEnabled(Tracers.class);
		if (blockEsp == null && tracers == null) return;

		float scale = (float) mc.getWindow().getGuiScale();
		double width = graphics.guiWidth() * scale;
		double height = graphics.guiHeight() * scale;
		float partialTick = tickCounter.getGameTimeDeltaPartialTick(true);
		Projection projection = new Projection(partialTick, width, height);

		graphics.pose().pushMatrix();
		graphics.pose().scale(1 / scale, 1 / scale);
		if (blockEsp != null) drawBlocks(graphics, projection, blockEsp);
		if (tracers != null) drawTracers(graphics, projection, tracers, partialTick, mc);
		graphics.pose().popMatrix();
	}

	private void drawBlocks(GuiGraphics g, Projection p, BlockESP esp) {
		double cx = p.width / 2, cy = p.height / 2;
		for (BlockESP.Found found : esp.found()) {
			int bx = found.pos().getX(), by = found.pos().getY(), bz = found.pos().getZ();
			// Screen rectangle around the block's 8 corners
			double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
			boolean visible = true;
			for (int i = 0; i < 8 && visible; i++) {
				double[] s = p.project(bx + (i & 1), by + ((i >> 1) & 1), bz + ((i >> 2) & 1));
				if (s == null) {
					visible = false;
					break;
				}
				minX = Math.min(minX, s[0]);
				minY = Math.min(minY, s[1]);
				maxX = Math.max(maxX, s[0]);
				maxY = Math.max(maxY, s[1]);
			}
			int color = found.color();
			if (visible && maxX > 0 && maxY > 0 && minX < p.width && minY < p.height) {
				int x1 = (int) minX, y1 = (int) minY, x2 = Math.max(x1 + 2, (int) maxX), y2 = Math.max(y1 + 2, (int) maxY);
				if (esp.fill()) g.fill(x1, y1, x2, y2, (color & 0x00FFFFFF) | 0x40000000);
				Draw.outline(g, x1, y1, x2, y2, color, 1);
			}
			if (esp.tracers()) {
				double[] end = p.towards(bx + 0.5, by + 0.5, bz + 0.5);
				Draw.line(g, cx, cy, end[0], end[1], (color & 0x00FFFFFF) | 0xA0000000, 1);
			}
		}
	}

	private void drawTracers(GuiGraphics g, Projection p, Tracers tracers, float partialTick, Minecraft mc) {
		double cx = p.width / 2, cy = p.height / 2;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!tracers.shouldTrace(entity)) continue;
			Vec3 pos = entity.getPosition(partialTick);
			double[] end = p.towards(pos.x, pos.y + entity.getBbHeight() / 2, pos.z);
			Draw.line(g, cx, cy, end[0], end[1], tracers.colorFor(entity), tracers.thickness());
		}
	}
}
