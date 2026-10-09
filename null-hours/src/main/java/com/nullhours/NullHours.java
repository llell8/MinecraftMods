package com.nullhours;

import com.nullhours.haunt.HauntCommand;
import com.nullhours.haunt.HauntDirector;
import com.nullhours.net.ScarePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NullHours implements ModInitializer {
	public static final String MOD_ID = "nullhours";
	public static final Logger LOGGER = LoggerFactory.getLogger("Null Hours");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		NullHoursConfig.load();
		ModEntities.register();
		PayloadTypeRegistry.playS2C().register(ScarePayload.TYPE, ScarePayload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(HauntDirector::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> HauntDirector.reset());
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> HauntCommand.register(dispatcher));
	}
}
