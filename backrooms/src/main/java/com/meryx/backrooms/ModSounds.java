package com.meryx.backrooms;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final SoundEvent HUM = register("ambient.hum");
    public static final SoundEvent LIGHT_BUZZ = register("block.fluorescent_light.buzz");
    public static final SoundEvent NOCLIP = register("player.noclip");
    public static final SoundEvent SMILER_AMBIENT = register("entity.smiler.ambient");
    public static final SoundEvent SMILER_SCREAM = register("entity.smiler.scream");
    public static final SoundEvent SMILER_BLINK = register("entity.smiler.blink");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(Backrooms.id(name));
        return Registry.register(BuiltInRegistries.SOUND_EVENT, Backrooms.id(name), event);
    }

    public static void init() {
    }
}
