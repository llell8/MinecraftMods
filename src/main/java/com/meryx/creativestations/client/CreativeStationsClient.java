package com.meryx.creativestations.client;

import com.meryx.creativestations.OpenStationPayload;
import com.meryx.creativestations.Station;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;

public class CreativeStationsClient implements ClientModInitializer {
    // Size of the vanilla creative inventory background
    private static final int WINDOW_WIDTH = 195;
    private static final int WINDOW_HEIGHT = 136;
    private static final int BUTTON_WIDTH = 48;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 1;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof CreativeModeInventoryScreen)) {
                return;
            }
            // A row of buttons just under the creative window
            int left = (scaledWidth - WINDOW_WIDTH) / 2;
            int y = (scaledHeight - WINDOW_HEIGHT) / 2 + WINDOW_HEIGHT + 4;
            for (Station station : Station.values()) {
                int x = left + station.ordinal() * (BUTTON_WIDTH + GAP);
                Button button = Button.builder(
                                Component.translatable("creativestations.station." + station.key),
                                b -> ClientPlayNetworking.send(new OpenStationPayload(station.ordinal())))
                        .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .tooltip(Tooltip.create(Component.translatable("creativestations.station." + station.key + ".tooltip")))
                        .build();
                Screens.getButtons(screen).add(button);
            }
        });
    }
}
