package com.meryx.creativestations.client;

import com.meryx.creativestations.OpenStationPayload;
import com.meryx.creativestations.Station;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A column of item-icon buttons beside the creative inventory and beside every station screen,
 * so you can jump between workstations without going back to the inventory.
 */
public class CreativeStationsClient implements ClientModInitializer {
    private static final int CREATIVE_WIDTH = 195;
    private static final int CREATIVE_HEIGHT = 136;
    private static final int STATION_WIDTH = 176;
    private static final int STATION_HEIGHT = 166;
    private static final int SIZE = 20;
    private static final int GAP = 2;
    private static final int MARGIN = 4;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (client.player == null || !client.player.isCreative()) {
                return;
            }
            if (screen instanceof CreativeModeInventoryScreen) {
                addColumn(screen, scaledWidth, scaledHeight, CREATIVE_WIDTH, CREATIVE_HEIGHT, null);
            } else {
                Station current = stationOf(screen);
                if (current != null) {
                    addColumn(screen, scaledWidth, scaledHeight, STATION_WIDTH, STATION_HEIGHT, current);
                }
            }
        });
    }

    private static Station stationOf(Screen screen) {
        if (screen instanceof CraftingScreen) return Station.CRAFTING;
        if (screen instanceof SmithingScreen) return Station.SMITHING;
        if (screen instanceof AnvilScreen) return Station.ANVIL;
        if (screen instanceof EnchantmentScreen) return Station.ENCHANTING;
        if (screen instanceof LoomScreen) return Station.LOOM;
        if (screen instanceof StonecutterScreen) return Station.STONECUTTER;
        if (screen instanceof CartographyTableScreen) return Station.CARTOGRAPHY;
        if (screen instanceof GrindstoneScreen) return Station.GRINDSTONE;
        return null;
    }

    /** Buttons down the right side of the window. {@code current} is greyed out, as you're already in it. */
    private static void addColumn(Screen screen, int scaledWidth, int scaledHeight, int windowWidth, int windowHeight, Station current) {
        int x = (scaledWidth - windowWidth) / 2 + windowWidth + MARGIN;
        int top = (scaledHeight - windowHeight) / 2;

        for (Station station : Station.values()) {
            int y = top + station.ordinal() * (SIZE + GAP);
            Button button = Button.builder(Component.empty(),
                            b -> ClientPlayNetworking.send(new OpenStationPayload(station.ordinal())))
                    .bounds(x, y, SIZE, SIZE)
                    .tooltip(Tooltip.create(Component.translatable("creativestations.station." + station.key)))
                    .build();
            button.active = station != current;
            Screens.getButtons(screen).add(button);

            // Draw the station's item on top of its button
            ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    graphics.renderItem(station.icon(), x + 2, y + 2));
        }

        // The item editor sits under the stations, on the creative inventory only
        if (current == null) {
            int y = top + Station.values().length * (SIZE + GAP) + 4;
            Button editor = Button.builder(Component.empty(),
                            b -> Minecraft.getInstance().setScreen(new ItemEditorScreen(screen)))
                    .bounds(x, y, SIZE, SIZE)
                    .tooltip(Tooltip.create(Component.translatable("creativestations.editor.tooltip")))
                    .build();
            Screens.getButtons(screen).add(editor);
            ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    graphics.renderItem(new ItemStack(Items.ENCHANTED_BOOK), x + 2, y + 2));
        }
    }
}
