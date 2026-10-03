package com.hackclient.module.modules.player;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public class AutoTool extends Module {
	public AutoTool() {
		super("AutoTool", "Switches to the best hotbar tool when mining.", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		if (!mc.options.keyAttack.isDown()) return;
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

		BlockState state = mc.level.getBlockState(hit.getBlockPos());
		Inventory inventory = mc.player.getInventory();

		int bestSlot = -1;
		float bestSpeed = inventory.getItem(inventory.getSelectedSlot()).getDestroySpeed(state);
		for (int i = 0; i < 9; i++) {
			float speed = inventory.getItem(i).getDestroySpeed(state);
			if (speed > bestSpeed) {
				bestSpeed = speed;
				bestSlot = i;
			}
		}
		if (bestSlot != -1) inventory.setSelectedSlot(bestSlot);
	}
}
