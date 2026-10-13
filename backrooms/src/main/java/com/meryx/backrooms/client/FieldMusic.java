package com.meryx.backrooms.client;

import com.meryx.backrooms.Backrooms;
import com.meryx.backrooms.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** An old record playing from nowhere, over and over, for as long as you are in The Field. */
public class FieldMusic extends AbstractTickableSoundInstance {
    public FieldMusic() {
        super(ModSounds.FIELD_MUSIC, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.8F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !client.level.dimension().equals(Backrooms.THE_FIELD)) {
            stop();
        }
    }
}
