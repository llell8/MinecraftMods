package com.nullhours.client;

import com.nullhours.NullHours;
import com.nullhours.net.ScarePayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;

/** Draws jumpscares, glitches and the other screen effects on top of the HUD. */
public final class ScareOverlay {
	private static final Identifier[] FACES = {
			NullHours.id("textures/gui/jumpscare/hollow.png"),
			NullHours.id("textures/gui/jumpscare/echo.png"),
			NullHours.id("textures/gui/jumpscare/grinner.png")
	};
	private static final int JUMPSCARE_TICKS = 16;
	private static final int FADE_TICKS = 5;
	private static final RandomSource RANDOM = RandomSource.create();

	private static int jumpscareTicks;
	private static int face;
	private static int glitchTicks;
	private static int flashTicks;
	private static int blackoutTicks;
	private static int blackoutLength;
	private static String text = "";

	private ScareOverlay() {
	}

	static void handle(ScarePayload payload) {
		Minecraft mc = Minecraft.getInstance();
		switch (payload.kind()) {
			case ScarePayload.JUMPSCARE -> {
				face = Math.floorMod(payload.variant(), FACES.length);
				jumpscareTicks = JUMPSCARE_TICKS;
				snapCamera(mc.player, payload.x(), payload.y(), payload.z());
				play(SoundEvents.ENDERMAN_SCREAM, 0.5f);
				play(SoundEvents.ELDER_GUARDIAN_CURSE, 0.6f);
				play(SoundEvents.WARDEN_ROAR, 1.6f);
			}
			case ScarePayload.GLITCH -> {
				glitchTicks = payload.variant();
				play(SoundEvents.BEACON_DEACTIVATE, 0.5f);
				play(SoundEvents.ENDERMAN_STARE, 1.8f);
			}
			case ScarePayload.FLASH -> {
				flashTicks = payload.variant();
				text = payload.text();
				play(SoundEvents.ELDER_GUARDIAN_CURSE, 1.5f);
			}
			case ScarePayload.BLACKOUT -> {
				blackoutTicks = blackoutLength = payload.variant();
				text = payload.text();
				play(SoundEvents.BEACON_DEACTIVATE, 0.5f);
			}
			default -> {
			}
		}
	}

	private static void play(SoundEvent sound, float pitch) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 1.0f));
	}

	/** Turns the player's head to face whatever just got them. */
	private static void snapCamera(LocalPlayer player, double x, double y, double z) {
		if (player == null) return;
		double dx = x - player.getX();
		double dy = y - player.getEyeY();
		double dz = z - player.getZ();
		if (dx * dx + dz * dz < 1.0e-4) return;
		float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		player.setYRot(yaw);
		player.setXRot(pitch);
		player.yRotO = yaw;
		player.xRotO = pitch;
		player.setYHeadRot(yaw);
	}

	static void tick() {
		if (jumpscareTicks > 0) jumpscareTicks--;
		if (glitchTicks > 0) glitchTicks--;
		if (flashTicks > 0) flashTicks--;
		if (blackoutTicks > 0) blackoutTicks--;
	}

	static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		Font font = Minecraft.getInstance().font;

		if (blackoutTicks > 0) {
			graphics.fill(0, 0, width, height, 0xFF000000);
			// The words only fade in halfway through, and flicker
			if (blackoutTicks < blackoutLength / 2 && RANDOM.nextInt(6) != 0) {
				graphics.drawCenteredString(font, Component.literal(text), width / 2, height / 2 - 4, 0xFF6E6E6E);
			}
		}

		if (glitchTicks > 0) drawGlitch(graphics, width, height);

		if (flashTicks > 0) {
			graphics.fill(0, 0, width, height, 0xE0000000);
			drawStatic(graphics, width, height, 120, 0x30);
			drawBigText(graphics, font, text, width, height, 0xFFE6E6E6, 4.0f);
		}

		if (jumpscareTicks > 0) drawJumpscare(graphics, width, height);
	}

	/**
	 * A short cut to the face: static, the face flickering in and out of the static, then
	 * black. Everything is black and white.
	 */
	private static void drawJumpscare(GuiGraphics graphics, int width, int height) {
		graphics.fill(0, 0, width, height, 0xFF000000);
		int elapsed = JUMPSCARE_TICKS - jumpscareTicks;
		if (jumpscareTicks <= FADE_TICKS) {
			drawStatic(graphics, width, height, 60 * jumpscareTicks, 0x50);
			return;
		}
		boolean showFace = elapsed >= 1 && (elapsed % 3 != 2 || elapsed > 6);
		if (showFace) {
			// Lunges at the screen and shakes
			float grow = Math.min(1.0f, elapsed / 3.0f);
			int size = (int) (height * (0.8f + 0.5f * grow));
			int shake = Math.max(2, size / 30);
			int x = (width - size) / 2 + RANDOM.nextInt(shake * 2 + 1) - shake;
			int y = (height - size) / 2 + RANDOM.nextInt(shake * 2 + 1) - shake;
			graphics.blit(RenderPipelines.GUI_TEXTURED, FACES[face], x, y, 0.0f, 0.0f, size, size, 128, 128, 128, 128);
			drawStatic(graphics, width, height, 500, 0x40);
		} else {
			drawStatic(graphics, width, height, 1500, 0xB0);
		}
		// Torn scanlines across the picture
		for (int i = 0; i < 4; i++) {
			int y = RANDOM.nextInt(height);
			graphics.fill(0, y, width, y + 1 + RANDOM.nextInt(3), RANDOM.nextBoolean() ? 0xC0000000 : 0x70FFFFFF);
		}
	}

	private static void drawGlitch(GuiGraphics graphics, int width, int height) {
		int bars = 6 + RANDOM.nextInt(10);
		for (int i = 0; i < bars; i++) {
			int y = RANDOM.nextInt(height);
			int h = 1 + RANDOM.nextInt(Math.max(2, height / 25));
			int x = RANDOM.nextInt(width / 3);
			int gray = RANDOM.nextInt(256);
			int color = (RANDOM.nextBoolean() ? 0xB0000000 : 0x80000000) | gray << 16 | gray << 8 | gray;
			graphics.fill(x, y, x + width / 2 + RANDOM.nextInt(width / 2 + 1), y + h, color);
		}
		drawStatic(graphics, width, height, 400, 0x80);
		if (RANDOM.nextInt(4) == 0) graphics.fill(0, 0, width, height, 0x60000000);
	}

	/** Blocky gray TV static. */
	private static void drawStatic(GuiGraphics graphics, int width, int height, int dots, int alpha) {
		int cell = Math.max(2, width / 160);
		for (int i = 0; i < dots; i++) {
			int x = RANDOM.nextInt(Math.max(1, width / cell)) * cell;
			int y = RANDOM.nextInt(Math.max(1, height / cell)) * cell;
			int gray = RANDOM.nextInt(256);
			graphics.fill(x, y, x + cell, y + cell, alpha << 24 | gray << 16 | gray << 8 | gray);
		}
	}

	private static void drawBigText(GuiGraphics graphics, Font font, String message, int width, int height, int color, float scale) {
		Component component = Component.literal(message);
		float fit = Math.min(scale, (width - 20) / (float) Math.max(1, font.width(component)));
		graphics.pose().pushMatrix();
		graphics.pose().translate(width / 2.0f + RANDOM.nextInt(5) - 2, height / 2.0f + RANDOM.nextInt(5) - 2);
		graphics.pose().scale(fit, fit);
		graphics.drawString(font, component, -font.width(component) / 2, -font.lineHeight / 2, color, false);
		graphics.pose().popMatrix();
	}
}
