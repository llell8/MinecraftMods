package com.hackclient.hud;

import com.hackclient.HackClient;
import com.hackclient.module.ModuleManager;
import com.hackclient.module.modules.render.BlockESP;
import com.hackclient.module.modules.render.ESP;
import com.hackclient.module.modules.render.Tracers;
import com.hackclient.render.Projection;
import com.hackclient.render.QuadBatch;
import com.hackclient.render.ShapeMode;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws ESP boxes, BlockESP boxes and Tracers on top of the game, so they show through walls.
 * Works in real screen pixels (undoing the GUI scale) so lines stay thin and smooth, and batches
 * everything into one draw.
 */
public class EspOverlay implements HudElement {
	@Override
	public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.options.hideGui) return;
		ModuleManager modules = HackClient.getModuleManager();
		ESP esp = modules.getIfEnabled(ESP.class);
		BlockESP blockEsp = modules.getIfEnabled(BlockESP.class);
		Tracers tracers = modules.getIfEnabled(Tracers.class);
		if ((esp == null || esp.mode() == ESP.Mode.GLOW) && blockEsp == null && tracers == null) return;

		float scale = (float) mc.getWindow().getGuiScale();
		int width = (int) Math.ceil(graphics.guiWidth() * scale);
		int height = (int) Math.ceil(graphics.guiHeight() * scale);
		float partialTick = tickCounter.getGameTimeDeltaPartialTick(true);
		Projection projection = new Projection(partialTick, width, height);

		graphics.pose().pushMatrix();
		graphics.pose().scale(1 / scale, 1 / scale);
		QuadBatch batch = new QuadBatch(graphics, width, height);

		if (blockEsp != null) drawBlocks(batch, projection, blockEsp);
		if (esp != null && esp.mode() != ESP.Mode.GLOW) drawEntities(batch, projection, esp, partialTick, mc);
		if (tracers != null) drawTracers(batch, projection, tracers, partialTick, mc);

		batch.submit(graphics);
		graphics.pose().popMatrix();
	}

	private void drawBlocks(QuadBatch batch, Projection p, BlockESP esp) {
		for (BlockESP.Found found : esp.found()) {
			AABB box = new AABB(found.pos());
			int color = found.color();
			p.box(batch, box, esp.shapeMode(), esp.sideColor(color), color, esp.lineWidth());
			if (esp.tracers()) {
				double[] end = p.towards(box.getCenter().x, box.getCenter().y, box.getCenter().z);
				batch.line(p.width / 2, p.height / 2, end[0], end[1], esp.lineWidth(), (color & 0x00FFFFFF) | 0xC0000000);
			}
		}
	}

	private void drawEntities(QuadBatch batch, Projection p, ESP esp, float partialTick, Minecraft mc) {
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!esp.shouldDraw(entity)) continue;
			// Move the box to where the entity is drawn this frame, so it doesn't lag behind
			Vec3 offset = entity.getPosition(partialTick).subtract(entity.position());
			AABB box = entity.getBoundingBox().move(offset);
			int line = esp.lineColor(entity);
			int side = esp.sideColor(entity);

			if (esp.mode() == ESP.Mode.BOX) {
				p.box(batch, box, esp.shapeMode(), side, line, esp.lineWidth());
			} else {
				double[] r = p.rect(box);
				if (r == null) continue;
				ShapeMode shape = esp.shapeMode();
				if (shape.sides()) batch.quad(r[0], r[1], r[2], r[1], r[2], r[3], r[0], r[3], side);
				if (shape.lines()) {
					double w = esp.lineWidth();
					batch.line(r[0], r[1], r[2], r[1], w, line);
					batch.line(r[2], r[1], r[2], r[3], w, line);
					batch.line(r[2], r[3], r[0], r[3], w, line);
					batch.line(r[0], r[3], r[0], r[1], w, line);
				}
			}
		}
	}

	private void drawTracers(QuadBatch batch, Projection p, Tracers tracers, float partialTick, Minecraft mc) {
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!tracers.shouldTrace(entity)) continue;
			Vec3 pos = entity.getPosition(partialTick);
			double[] end = p.towards(pos.x, pos.y + entity.getBbHeight() * tracers.targetHeight(), pos.z);
			batch.line(p.width / 2, p.height / 2, end[0], end[1], tracers.lineWidth(), tracers.colorFor(entity));
		}
	}
}
