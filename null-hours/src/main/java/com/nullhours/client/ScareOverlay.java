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
	private static final int JUMPSCARE_TICKS = 18;
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
				play(SoundEvents.GHAST_SCREAM, 0.6f);
				play(SoundEvents.ELDER_GUARDIAN_CURSE, 0.8f);
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
				graphics.drawCenteredString(font, Component.literal(text), width / 2, height / 2 - 4, 0xFF5A0000);
			}
		}

		if (glitchTicks > 0) drawGlitch(graphics, width, height);

		if (flashTicks > 0) {
			graphics.fill(0, 0, width, height, 0xC0000000);
			drawBigText(graphics, font, text, width, height, 0xFFC00000, 4.0f);
		}

		if (jumpscareTicks > 0) drawJumpscare(graphics, width, height);
	}

	private static void drawJumpscare(GuiGraphics graphics, int width, int height) {
		graphics.fill(0, 0, width, height, 0xFF000000);
		int elapsed = JUMPSCARE_TICKS - jumpscareTicks;
		// The face lunges at the screen over the first few frames and shakes
		float grow = Math.min(1.0f, elapsed / 4.0f);
		int size = (int) (height * (0.7f + 0.6f * grow));
		int shake = Math.max(2, size / 40);
		int x = (width - size) / 2 + RANDOM.nextInt(shake * 2 + 1) - shake;
		int y = (height - size) / 2 + RANDOM.nextInt(shake * 2 + 1) - shake;
		graphics.blit(RenderPipelines.GUI_TEXTURED, FACES[face], x, y, 0.0f, 0.0f, size, size, 64, 64, 64, 64);
		if (elapsed % 4 < 2) graphics.fill(0, 0, width, height, 0x50FF0000);
		if (jumpscareTicks < 5) graphics.fill(0, 0, width, height, (255 - jumpscareTicks * 50) << 24);
	}

	private static void drawGlitch(GuiGraphics graphics, int width, int height) {
		int bars = 6 + RANDOM.nextInt(10);
		for (int i = 0; i < bars; i++) {
			int y = RANDOM.nextInt(height);
			int h = 1 + RANDOM.nextInt(Math.max(2, height / 25));
			int x = RANDOM.nextInt(width / 3);
			int color = switch (RANDOM.nextInt(4)) {
				case 0 -> 0xA0FF0000;
				case 1 -> 0xA000FFFF;
				case 2 -> 0xC0000000;
				default -> 0x90FFFFFF;
			};
			graphics.fill(x, y, x + width / 2 + RANDOM.nextInt(width / 2 + 1), y + h, color);
		}
		// Blocky static
		int cell = Math.max(2, width / 160);
		for (int i = 0; i < 400; i++) {
			int x = RANDOM.nextInt(width / cell) * cell;
			int y = RANDOM.nextInt(height / cell) * cell;
			int gray = RANDOM.nextInt(256);
			graphics.fill(x, y, x + cell, y + cell, 0x80000000 | gray << 16 | gray << 8 | gray);
		}
		if (RANDOM.nextInt(4) == 0) graphics.fill(0, 0, width, height, 0x60000000);
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
