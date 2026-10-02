package com.meryx.creativestations;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.SmithingMenu;

/** The workstations that can be opened from the creative inventory. */
public enum Station {
    CRAFTING("crafting", "container.crafting", Items.CRAFTING_TABLE),
    SMITHING("smithing", "container.upgrade", Items.SMITHING_TABLE),
    ANVIL("anvil", "container.repair", Items.ANVIL),
    ENCHANTING("enchanting", "container.enchant", Items.ENCHANTING_TABLE),
    LOOM("loom", "container.loom", Items.LOOM),
    STONECUTTER("stonecutter", "container.stonecutter", Items.STONECUTTER),
    CARTOGRAPHY("cartography", "container.cartography_table", Items.CARTOGRAPHY_TABLE),
    GRINDSTONE("grindstone", "container.grindstone_title", Items.GRINDSTONE);

    public final String key;
    private final String title;
    private final Item icon;

    Station(String key, String title, Item icon) {
        this.key = key;
        this.title = title;
        this.icon = icon;
    }

    public ItemStack icon() {
        return new ItemStack(icon);
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
            case LOOM -> new LoomMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player player) {
                    return true;
                }
            };
            case STONECUTTER -> new StonecutterMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player player) {
                    return true;
                }
            };
            case CARTOGRAPHY -> new CartographyTableMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player player) {
                    return true;
                }
            };
            case GRINDSTONE -> new GrindstoneMenu(id, inv, access) {
                @Override
                public boolean stillValid(Player player) {
                    return true;
                }
            };
        };
    }
}
