package com.nullhours.net;

import com.nullhours.NullHours;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells the client to play a screen effect.
 *
 * @param kind    one of the constants below
 * @param variant which face for a jumpscare, or the length in ticks for other effects
 * @param x       where the jumpscare came from, so the camera can snap towards it
 * @param text    words shown by FLASH and BLACKOUT
 */
public record ScarePayload(int kind, int variant, double x, double y, double z, String text) implements CustomPacketPayload {
	public static final int JUMPSCARE = 0;
	public static final int GLITCH = 1;
	public static final int FLASH = 2;
	public static final int BLACKOUT = 3;

	public static final int FACE_HOLLOW = 0;
	public static final int FACE_ECHO = 1;
	public static final int FACE_GRINNER = 2;

	public static final Type<ScarePayload> TYPE = new Type<>(NullHours.id("scare"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ScarePayload> CODEC =
			CustomPacketPayload.codec(ScarePayload::write, ScarePayload::new);

	private ScarePayload(RegistryFriendlyByteBuf buf) {
		this(buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readUtf());
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(kind);
		buf.writeVarInt(variant);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeUtf(text);
	}

	public static ScarePayload effect(int kind, int ticks, String text) {
		return new ScarePayload(kind, ticks, 0, 0, 0, text);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
