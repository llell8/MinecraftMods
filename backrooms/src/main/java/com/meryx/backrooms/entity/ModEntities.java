package com.meryx.backrooms.entity;

import com.meryx.backrooms.Backrooms;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/** Two kinds of Smiler that behave the same and only look different, and the Bacteria. */
public final class ModEntities {
    private static final List<Item> SPAWN_EGGS = new ArrayList<>();

    public static final EntityType<Smiler> SMILER = register("smiler",
            EntityType.Builder.of(Smiler::new, MobCategory.MONSTER).sized(0.6F, 2.6F).eyeHeight(2.35F));
    public static final EntityType<Smiler> VHS_SMILER = register("vhs_smiler",
            EntityType.Builder.of(Smiler::new, MobCategory.MONSTER).sized(0.6F, 2.6F).eyeHeight(2.35F));
    public static final EntityType<Bacteria> BACTERIA = register("bacteria",
            EntityType.Builder.of(Bacteria::new, MobCategory.MONSTER).sized(0.8F, 3.1F).eyeHeight(2.8F));

    private ModEntities() {
    }

    private static <T extends Mob> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Backrooms.id(name));
        EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.clientTrackingRange(10).build(key));
        ResourceKey<Item> eggKey = ResourceKey.create(Registries.ITEM, Backrooms.id(name + "_spawn_egg"));
        SPAWN_EGGS.add(Registry.register(BuiltInRegistries.ITEM, eggKey,
                new SpawnEggItem(new Item.Properties().setId(eggKey).spawnEgg(type))));
        return type;
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(SMILER, Smiler.createAttributes());
        FabricDefaultAttributeRegistry.register(VHS_SMILER, Smiler.createAttributes());
        FabricDefaultAttributeRegistry.register(BACTERIA, Bacteria.createAttributes());
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> SPAWN_EGGS.forEach(entries::accept));
    }
}
