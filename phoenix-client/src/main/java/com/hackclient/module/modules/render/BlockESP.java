package com.hackclient.module.modules.render;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.BlockListSetting;
import com.hackclient.setting.BoolSetting;
import com.hackclient.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlights chosen blocks through walls. Pick any blocks in the game in its settings.
 * The area around you is scanned a slice at a time, so it never freezes the game. Drawn by EspOverlay.
 */
public class BlockESP extends Module {
	private static final int CHECKS_PER_TICK = 60_000;

	private final BlockListSetting blocks = blocks("Blocks",
			"minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:ancient_debris",
			"minecraft:emerald_ore", "minecraft:deepslate_emerald_ore", "minecraft:spawner",
			"minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "minecraft:ender_chest", "minecraft:shulker_box");
	private final NumberSetting range = number("Range", 48, 8, 128, 0);
	private final NumberSetting maxShown = number("Max shown", 300, 10, 2000, 0);
	private final BoolSetting fill = bool("Fill", true);
	private final BoolSetting tracers = bool("Tracers", false);

	/** A found block and the colour to draw it in. */
	public record Found(BlockPos pos, int color) {
	}

	private List<Found> found = new ArrayList<>();
	private List<Found> scanning = new ArrayList<>();
	private BlockPos scanCenter;
	private int scanRadius;
	private int cx, cy, cz; // scan cursor, relative to scanCenter
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

	@Override
	public void onTick() {
		if (mc.level == null || mc.player == null || blocks.isEmpty()) {
			found = new ArrayList<>();
			return;
		}
		if (scanDone) startScan();

		int minY = mc.level.getMinY();
		int maxY = mc.level.getMaxY();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int budget = CHECKS_PER_TICK;
		int limit = maxShown.get().intValue();

		while (budget-- > 0 && !scanDone) {
			int y = scanCenter.getY() + cy;
			if (y >= minY && y <= maxY) {
				pos.set(scanCenter.getX() + cx, y, scanCenter.getZ() + cz);
				Block block = mc.level.getBlockState(pos).getBlock();
				if (blocks.contains(block) && scanning.size() < limit) {
					scanning.add(new Found(pos.immutable(), colorFor(block)));
				}
			}
			advance();
		}
		if (scanDone) {
			// Closest first, so "Max shown" keeps the nearest ones
			scanning.sort((a, b) -> Double.compare(a.pos().distSqr(scanCenter), b.pos().distSqr(scanCenter)));
			found = scanning;
			scanning = new ArrayList<>();
		}
	}

	private void startScan() {
		scanCenter = mc.player.blockPosition();
		scanRadius = range.get().intValue();
		cx = -scanRadius;
		cy = -scanRadius;
		cz = -scanRadius;
		scanDone = false;
	}

	private void advance() {
		if (++cx > scanRadius) {
			cx = -scanRadius;
			if (++cz > scanRadius) {
				cz = -scanRadius;
				if (++cy > scanRadius) scanDone = true;
			}
		}
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

	public boolean fill() {
		return fill.get();
	}

	public boolean tracers() {
		return tracers.get();
	}
}
