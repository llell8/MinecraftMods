package com.meryx.creativestations;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: "open this station for me". */
public record OpenStationPayload(int station) implements CustomPacketPayload {
    public static final Type<OpenStationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CreativeStations.MOD_ID, "open_station"));
    public static final StreamCodec<ByteBuf, OpenStationPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(OpenStationPayload::new, OpenStationPayload::station);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
