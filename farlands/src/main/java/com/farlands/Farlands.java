package com.farlands;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Farlands - a horror mod. Nothing is added to the world you can craft or hold;
 * instead the world slowly starts acting wrong around each player.
 */
public class Farlands implements ModInitializer {
    public static final String MOD_ID = "farlands";
    public static final Logger LOGGER = LoggerFactory.getLogger("Farlands");

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                HorrorEvents.tick(player);
            }
            HorrorEvents.tickQueue();
            Watcher.tick(server);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            HorrorEvents.forget(handler.getPlayer());
            Watcher.vanish(handler.getPlayer(), false);
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> Watcher.onEntityLoad(entity));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> Watcher.vanishAll());

        // /farlands <event> - plays an event on yourself, handy for testing
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("farlands")
                        .then(Commands.argument("event", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String name : HorrorEvents.EVENT_NAMES) builder.suggest(name);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "event");
                                    if (!HorrorEvents.run(player, name)) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown event: " + name));
                                        return 0;
                                    }
                                    return 1;
                                }))));

        LOGGER.info("Farlands loaded. Don't look behind you.");
    }
}
