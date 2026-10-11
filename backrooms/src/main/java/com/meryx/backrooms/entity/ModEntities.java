package com.meryx.backrooms.entity;

import com.meryx.backrooms.Backrooms;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    private static final ResourceKey<EntityType<?>> SMILER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Backrooms.id("smiler"));

    public static final EntityType<Smiler> SMILER = Registry.register(BuiltInRegistries.ENTITY_TYPE, SMILER_KEY,
            EntityType.Builder.of(Smiler::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.6F)
                    .eyeHeight(2.35F)
                    .clientTrackingRange(10)
                    .build(SMILER_KEY));

    private ModEntities() {
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(SMILER, Smiler.createAttributes());
    }
}
