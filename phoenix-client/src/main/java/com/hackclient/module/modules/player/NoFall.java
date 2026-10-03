package com.hackclient.module.modules.player;

import com.hackclient.module.Category;
import com.hackclient.module.Module;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.lwjgl.glfw.GLFW;

public class NoFall extends Module {
	public NoFall() {
		super("NoFall", "Prevents fall damage.", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onTick() {
		if (mc.player.fallDistance > 2.0 && !mc.player.isFallFlying()) {
			mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
		}
	}
}
