package com.meryx.creativestations;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class CreativeStations implements ModInitializer {
    public static final String MOD_ID = "creativestations";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(OpenStationPayload.TYPE, OpenStationPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(OpenStationPayload.TYPE, (payload, context) -> {
            Station station = Station.byId(payload.station());
            // Creative players only, so this can't be used to get free anvils in survival
            if (station != null && context.player().isCreative()) {
                context.server().execute(() -> station.open(context.player()));
            }
        });
    }
}
