package com.meryx.backrooms.entity;

import com.meryx.backrooms.Backrooms;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

public final class ModEntities {
    private static final ResourceKey<EntityType<?>> SMILER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Backrooms.id("smiler"));

    public static final EntityType<Smiler> SMILER = Registry.register(BuiltInRegistries.ENTITY_TYPE, SMILER_KEY,
            EntityType.Builder.of(Smiler::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.6F)
                    .eyeHeight(2.35F)
                    .clientTrackingRange(10)
                    .build(SMILER_KEY));

    private static final ResourceKey<Item> SMILER_EGG_KEY = ResourceKey.create(Registries.ITEM, Backrooms.id("smiler_spawn_egg"));
    public static final Item SMILER_SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM, SMILER_EGG_KEY,
            new SpawnEggItem(SMILER, new Item.Properties().setId(SMILER_EGG_KEY)));

    private ModEntities() {
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(SMILER, Smiler.createAttributes());
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(SMILER_SPAWN_EGG));
    }
}
