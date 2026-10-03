package com.hackclient.gui;

import com.hackclient.HackClient;
import com.hackclient.config.Config;
import com.hackclient.module.modules.misc.ClickGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Base for our screens: draws everything at ClickGUI's "GUI size" (independent of Minecraft's own GUI scale),
 * and converts mouse positions into that scaled space. Subclasses work in scaled coordinates
 * and use {@link #scaledWidth()} / {@link #scaledHeight()} instead of width / height.
 */
public abstract class ScaledScreen extends Screen {
	protected ScaledScreen(String title) {
		super(Component.literal(title));
	}

	/** Real screen pixels per GUI pixel, from the GUI size setting. */
	protected double pixelScale() {
		ClickGui clickGui = HackClient.getModuleManager().get(ClickGui.class);
		return clickGui != null ? clickGui.getSize().pixels : GuiSize.NORMAL.pixels;
	}

	/** Extra scale on top of Minecraft's GUI scale to reach {@link #pixelScale()}. */
	protected float scale() {
		return (float) (pixelScale() / minecraft.getWindow().getGuiScale());
	}

	protected int scaledWidth() {
		return (int) (width / scale());
	}

	protected int scaledHeight() {
		return (int) (height / scale());
	}

	@Override
	public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick); // background blur
		Theme.update();
		Text.setPixelScale(pixelScale()); // pick the font sharpness that matches our size
		float scale = scale();
		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		renderScaled(graphics, (int) (mouseX / scale), (int) (mouseY / scale));
		graphics.pose().popMatrix();
		Text.setPixelScale(minecraft.getWindow().getGuiScale());
	}

	protected abstract void renderScaled(GuiGraphics graphics, int mouseX, int mouseY);

	protected boolean onClick(double mouseX, double mouseY, int button) {
		return false;
	}

	protected void onRelease() {}

	protected void onDrag(double mouseX, double mouseY) {}

	protected boolean onScroll(double mouseX, double mouseY, double amount) {
		return false;
	}

	protected boolean onKey(int key) {
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (onClick(event.x() / scale(), event.y() / scale(), event.button())) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		onRelease();
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		onDrag(event.x() / scale(), event.y() / scale());
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (onScroll(mouseX / scale(), mouseY / scale(), scrollY)) return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (onKey(event.key())) return true;
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		Config.save(HackClient.getModuleManager());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
