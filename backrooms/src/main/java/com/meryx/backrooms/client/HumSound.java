package com.meryx.backrooms.client;

import com.meryx.backrooms.Backrooms;
import com.meryx.backrooms.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** The endless buzz of the fluorescent lights, heard everywhere in Level 0. */
public class HumSound extends AbstractTickableSoundInstance {
    public HumSound() {
        super(ModSounds.HUM, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.4F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !client.level.dimension().equals(Backrooms.LEVEL_0)) {
            stop();
        }
    }
}
