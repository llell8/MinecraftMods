package com.meryx.backrooms;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class ModSounds {
    public static final SoundEvent HUM = register("ambient.hum");
    public static final SoundEvent LIGHT_BUZZ = register("block.fluorescent_light.buzz");
    public static final SoundEvent NOCLIP = register("player.noclip");
    public static final SoundEvent SMILER_AMBIENT = register("entity.smiler.ambient");
    public static final SoundEvent SMILER_SCREAM = register("entity.smiler.scream");
    public static final SoundEvent SMILER_BLINK = register("entity.smiler.blink");
    public static final SoundEvent GLITCH = register("block.glitch.glitch");
    public static final SoundEvent BACTERIA_AMBIENT = register("entity.bacteria.ambient");
    public static final SoundEvent BACTERIA_HURT = register("entity.bacteria.hurt");
    public static final SoundEvent BACTERIA_DEATH = register("entity.bacteria.death");
    public static final SoundEvent BACTERIA_SCREAM = register("entity.bacteria.scream");
    public static final SoundEvent BACTERIA_MIMIC = register("entity.bacteria.mimic");
    public static final SoundEvent FIELD_MUSIC = register("music.the_field");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(Backrooms.id(name));
        return Registry.register(BuiltInRegistries.SOUND_EVENT, Backrooms.id(name), event);
    }

    /** Plays a sound to one player only, at their own position, so it is at full volume wherever its source is. */
    public static void playInEars(ServerPlayer player, SoundEvent event, float pitch) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event), SoundSource.HOSTILE,
                player.getX(), player.getEyeY(), player.getZ(), 1.0F, pitch, player.getRandom().nextLong()));
    }

    public static void init() {
    }
}
