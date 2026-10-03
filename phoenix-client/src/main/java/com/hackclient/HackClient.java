package com.hackclient;

import com.hackclient.config.Config;
import com.hackclient.hud.ModuleListHud;
import com.hackclient.hud.PlayerTrackerHud;
import com.hackclient.hud.TradePreviewHud;
import com.hackclient.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HackClient implements ClientModInitializer {
	public static final String MOD_ID = "hackclient";
	public static final String NAME = "Phoenix Client";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	private static ModuleManager moduleManager;

	@Override
	public void onInitializeClient() {
		moduleManager = new ModuleManager();
		Config.load(moduleManager);

		ClientTickEvents.END_CLIENT_TICK.register(moduleManager::onTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save(moduleManager));
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "module_list"), new ModuleListHud());
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "trade_preview"), new TradePreviewHud());
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "player_tracker"), new PlayerTrackerHud());

		LOGGER.info("{} loaded with {} modules", NAME, moduleManager.getModules().size());
	}

	public static ModuleManager getModuleManager() {
		return moduleManager;
	}
}
