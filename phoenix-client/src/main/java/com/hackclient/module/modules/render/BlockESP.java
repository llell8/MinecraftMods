package com.hackclient.module.modules.render;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BlockListSetting;
import com.hackclient.render.ShapeMode;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.ColorSetting;
import com.hackclient.setting.ModeSetting;
import com.hackclient.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Meteor-style block ESP: 3D boxes around chosen blocks, visible through walls. Pick any blocks in the game in its settings.
 * The area around you is rescanned continuously, a few chunks per tick. Drawn by EspOverlay.
 */
public class BlockESP extends Module {
	private final BlockListSetting blocks = blocks("Blocks",
			"minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:ancient_debris",
			"minecraft:emerald_ore", "minecraft:deepslate_emerald_ore", "minecraft:spawner",
			"minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "minecraft:ender_chest", "minecraft:shulker_box");
	private final NumberSetting range = number("Range", 48, 8, 128, 0);
	private final NumberSetting maxShown = number("Max shown", 300, 10, 2000, 0);
	private final ModeSetting<ShapeMode> shapeMode = mode("Shape", ShapeMode.BOTH);
	private final NumberSetting fillOpacity = number("Fill opacity", 40, 0, 255, 0);
	private final NumberSetting lineWidth = number("Line width", 1.5, 0.5, 4, 1);
	private final BoolSetting tracers = bool("Tracers", false);
	private final BoolSetting customColor = bool("Custom colour", false);
	// Picking a colour switches "Custom colour" on, so the picked colour is what you see
	private final ColorSetting color = color("Colour", 0xFF00FFFF).onPicked(() -> customColor.set(true));

	/** A found block and the colour to draw it in. */
	public record Found(BlockPos pos, int color) {
	}

	private static final int CHUNKS_PER_TICK = 24;

	private List<Found> found = new ArrayList<>();
	private List<Found> scanning = new ArrayList<>();
	private BlockPos scanCenter;
	private int scanRadius;
	private int chunkRadius;
	private int chunkIndex; // which chunk of the square around you we're on this pass
	private boolean scanDone = true;

	public BlockESP() {
		super("BlockESP", "Highlights the blocks you pick (any block in the game) through walls.", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		found = new ArrayList<>();
		scanning = new ArrayList<>();
		scanDone = true;
	}

	/**
	 * Scans the chunks around you a few per tick. Each 16x16x16 section keeps a palette of the block
	 * types in it, so sections that can't contain any picked block are skipped without looking at a
	 * single block. That makes a full pass take a fraction of a second.
	 */
	@Override
	public void onTick() {
		if (mc.level == null || mc.player == null || blocks.isEmpty()) {
			found = new ArrayList<>();
			return;
		}
		if (scanDone) startScan();

		int side = chunkRadius * 2 + 1;
		int limit = maxShown.get().intValue();
		for (int n = 0; n < CHUNKS_PER_TICK && !scanDone; n++) {
			int cx = (scanCenter.getX() >> 4) - chunkRadius + chunkIndex % side;
			int cz = (scanCenter.getZ() >> 4) - chunkRadius + chunkIndex / side;
			if (mc.level.getChunkSource().hasChunk(cx, cz)) scanChunk(mc.level.getChunk(cx, cz), limit);
			if (++chunkIndex >= side * side) scanDone = true;
		}
		if (scanDone) {
			// Closest first, so "Max shown" keeps the nearest ones
			scanning.sort((a, b) -> Double.compare(a.pos().distSqr(scanCenter), b.pos().distSqr(scanCenter)));
			if (scanning.size() > limit) scanning = new ArrayList<>(scanning.subList(0, limit));
			found = scanning;
			scanning = new ArrayList<>();
		}
	}

	private void scanChunk(LevelChunk chunk, int limit) {
		LevelChunkSection[] sections = chunk.getSections();
		int minY = scanCenter.getY() - scanRadius;
		int maxY = scanCenter.getY() + scanRadius;
		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection section = sections[i];
			int sectionY = mc.level.getSectionYFromSectionIndex(i) << 4;
			if (sectionY + 15 < minY || sectionY > maxY) continue;
			if (section.hasOnlyAir() || !section.maybeHas(state -> blocks.contains(state.getBlock()))) continue;

			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						Block block = section.getBlockState(x, y, z).getBlock();
						if (!blocks.contains(block)) continue;
						BlockPos pos = new BlockPos(baseX + x, sectionY + y, baseZ + z);
						if (Math.abs(pos.getX() - scanCenter.getX()) > scanRadius || Math.abs(pos.getY() - scanCenter.getY()) > scanRadius
								|| Math.abs(pos.getZ() - scanCenter.getZ()) > scanRadius) continue;
						scanning.add(new Found(pos, colorFor(block)));
						if (scanning.size() > limit * 4) return; // plenty; the closest are kept after sorting
					}
				}
			}
		}
	}

	private void startScan() {
		scanCenter = mc.player.blockPosition();
		scanRadius = range.get().intValue();
		chunkRadius = (scanRadius >> 4) + 1;
		chunkIndex = 0;
		scanDone = false;
	}

	/** The block's map colour, so diamonds are cyan, gold is yellow, and so on. */
	private int colorFor(Block block) {
		int rgb = block.defaultMapColor().col;
		if (rgb == 0) rgb = 0xFFFFFF;
		return 0xFF000000 | rgb;
	}

	public List<Found> found() {
		return found;
	}

	public ShapeMode shapeMode() {
		return shapeMode.get();
	}

	public double lineWidth() {
		return lineWidth.get();
	}

	/** The block's own colour, or the picked one if "Custom colour" is on. */
	public int lineColor(Found found) {
		return customColor.get() ? color.get() : found.color();
	}

	public int sideColor(int lineColor) {
		return (lineColor & 0x00FFFFFF) | (fillOpacity.get().intValue() << 24);
	}

	public boolean tracers() {
		return tracers.get();
	}
}
