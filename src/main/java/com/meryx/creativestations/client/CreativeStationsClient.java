package com.meryx.creativestations.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Adds an anvil button beside the creative inventory that opens the Creative Anvil. */
public class CreativeStationsClient implements ClientModInitializer {
    private static final int CREATIVE_WIDTH = 195;
    private static final int CREATIVE_HEIGHT = 136;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof CreativeModeInventoryScreen) || client.player == null || !client.player.isCreative()) {
                return;
            }
            int x = (scaledWidth - CREATIVE_WIDTH) / 2 + CREATIVE_WIDTH + 4;
            int y = (scaledHeight - CREATIVE_HEIGHT) / 2;
            Button button = Button.builder(Component.empty(), b -> Minecraft.getInstance().setScreen(new CreativeAnvilScreen(screen)))
                    .bounds(x, y, 20, 20)
                    .tooltip(Tooltip.create(Component.translatable("creativestations.editor.tooltip")))
                    .build();
            Screens.getButtons(screen).add(button);
            ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    graphics.renderItem(new ItemStack(Items.ANVIL), x + 2, y + 2));
        });
    }
}
