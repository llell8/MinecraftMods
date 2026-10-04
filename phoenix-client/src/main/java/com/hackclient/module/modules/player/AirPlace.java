package com.hackclient.module.modules.player;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import com.hackclient.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Place blocks in mid-air: when you're holding a block and looking at nothing, right-click places it
 * in front of you at the chosen distance. The spot is shown with a box (drawn by EspOverlay).
 */
public class AirPlace extends Module {
	private static final int PLACE_DELAY = 4; // ticks between placements while holding right-click, like vanilla

	private final NumberSetting range = number("Range", 4, 1, 6, 1);

	private int cooldown;
	private BlockPos target;

	public AirPlace() {
		super("AirPlace", "Place blocks in mid-air where you're looking.", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	protected void onDisable() {
		target = null;
	}

	@Override
	public void onTick() {
		if (cooldown > 0) cooldown--;
		target = findTarget();
		if (target == null || !mc.options.keyUse.isDown() || cooldown > 0) return;

		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
		mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
		mc.player.swing(InteractionHand.MAIN_HAND);
		cooldown = PLACE_DELAY;
	}

	/** The air block in front of you, if you're holding a block and not looking at anything. */
	private BlockPos findTarget() {
		if (mc.screen != null || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.MISS) return null;
		if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return null;
		Vec3 spot = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(range.get()));
		BlockPos pos = BlockPos.containing(spot);
		return mc.level.getBlockState(pos).canBeReplaced() ? pos : null;
	}

	/** Where a block would go right now, for the preview box. */
	public BlockPos target() {
		return target;
	}
}
