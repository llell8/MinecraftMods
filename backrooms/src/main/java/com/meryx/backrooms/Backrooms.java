package com.meryx.backrooms;

import com.meryx.backrooms.block.ModBlocks;
import com.meryx.backrooms.entity.ModEntities;
import com.meryx.backrooms.world.Level0ChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** The Backrooms: noclip out of the overworld into an endless, humming Level 0. */
public class Backrooms implements ModInitializer {
    public static final String MOD_ID = "backrooms";
    public static final ResourceKey<Level> LEVEL_0 = ResourceKey.create(Registries.DIMENSION, id("level_0"));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.init();
        ModBlocks.init();
        ModEntities.init();
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("level_0"), Level0ChunkGenerator.CODEC);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(Noclip::onDamage);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Noclip.tick(server);
            SmilerSpawner.tick(server);
        });
    }
}
