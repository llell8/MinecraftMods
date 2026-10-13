package com.meryx.backrooms;

import com.meryx.backrooms.block.ModBlocks;
import com.meryx.backrooms.entity.ModEntities;
import com.meryx.backrooms.world.BackroomsChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import java.util.List;
import net.minecraft.world.level.Level;

/** The Backrooms: noclip out of the overworld into an endless, humming Level 0. */
public class Backrooms implements ModInitializer {
    public static final String MOD_ID = "backrooms";
    public static final ResourceKey<Level> LEVEL_0 = ResourceKey.create(Registries.DIMENSION, id("level_0"));
    public static final ResourceKey<Level> LEVEL_1 = ResourceKey.create(Registries.DIMENSION, id("level_1"));
    public static final ResourceKey<Level> LEVEL_2 = ResourceKey.create(Registries.DIMENSION, id("level_2"));
    public static final List<ResourceKey<Level>> LEVELS = List.of(LEVEL_0, LEVEL_1, LEVEL_2);
    /** Flat grass, a house in the distance, and music from somewhere. Not one of the levels: nothing hunts you there. */
    public static final ResourceKey<Level> THE_FIELD = ResourceKey.create(Registries.DIMENSION, id("the_field"));
    /** Every dimension this mod generates, in the order of their generator types. */
    public static final List<ResourceKey<Level>> DIMENSIONS = List.of(LEVEL_0, LEVEL_1, LEVEL_2, THE_FIELD);

    /** Whether a dimension is one of the levels of the Backrooms. */
    public static boolean isBackrooms(ResourceKey<Level> dimension) {
        return LEVELS.contains(dimension);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.init();
        ModBlocks.init();
        ModEntities.init();
        List<String> generators = List.of("level_0", "level_1", "level_2", "the_field");
        for (int i = 0; i < generators.size(); i++) {
            Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id(generators.get(i)), BackroomsChunkGenerator.CODECS.get(i));
        }

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(Noclip::onDamage);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Glitches.tick(server);
            Noclip.tick(server);
            SmilerSpawner.tick(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> Glitches.endAll());
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> Glitches.onEntityLoad(entity));
    }
}
