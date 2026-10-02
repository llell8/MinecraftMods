package com.meryx.creativestations;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.SmithingMenu;

/** The workstations that can be opened from the creative inventory. */
public enum Station {
    CRAFTING("crafting", "container.crafting"),
    SMITHING("smithing", "container.upgrade"),
    ANVIL("anvil", "container.repair"),
    ENCHANTING("enchanting", "container.enchant");

    public final String key;
    private final String title;

    Station(String key, String title) {
        this.key = key;
        this.title = title;
    }

    public static Station byId(int id) {
        Station[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }

    /** Opens this station's menu for the player. The menus never close for being too far from a block. */
    public void open(ServerPlayer player) {
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, p) -> create(containerId, inventory, ContainerLevelAccess.create(player.level(), player.blockPosition())),
                Component.translatable(title));
        player.openMenu(provider);
    }

    private AbstractContainerMenu create(int id, net.minecraft.world.entity.player.Inventory inv, ContainerLevelAccess access) {
        return switch (this) {
            case CRAFTING -> new CraftingMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player player) {
                    return true;
                }
            };
            case SMITHING -> new SmithingMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player player) {
                    return true;
                }
            };
            case ANVIL -> new AnvilMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player player) {
                    return true;
                }
            };
            case ENCHANTING -> new EnchantmentMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player player) {
                    return true;
                }
            };
        };
    }
}
