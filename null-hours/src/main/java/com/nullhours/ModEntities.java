package com.nullhours;

import com.nullhours.entity.EchoEntity;
import com.nullhours.entity.GrinnerEntity;
import com.nullhours.entity.HollowEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<HollowEntity> HOLLOW = register("hollow",
			EntityType.Builder.of(HollowEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(16));
	public static final EntityType<EchoEntity> ECHO = register("echo",
			EntityType.Builder.of(EchoEntity::new, MobCategory.MONSTER).sized(0.6f, 1.8f).clientTrackingRange(10));
	public static final EntityType<GrinnerEntity> GRINNER = register("grinner",
			EntityType.Builder.of(GrinnerEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10));

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, NullHours.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(HOLLOW, HollowEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ECHO, EchoEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(GRINNER, GrinnerEntity.createAttributes());
	}
}
