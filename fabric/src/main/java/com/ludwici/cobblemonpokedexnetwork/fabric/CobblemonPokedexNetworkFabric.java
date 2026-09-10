package com.ludwici.cobblemonpokedexnetwork.fabric;

import com.ludwici.cobblemonpokedexnetwork.CobblemonPokedexNetwork;
import com.ludwici.cobblemonpokedexnetwork.network.PokedexChunkDataS2C;
import com.ludwici.cobblemonpokedexnetwork.network.PokedexSingleDataS2C;
import com.ludwici.cobblemonpokedexnetwork.platform.PlatformNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class CobblemonPokedexNetworkFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(PokedexChunkDataS2C.ID, PokedexChunkDataS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PokedexSingleDataS2C.ID, PokedexSingleDataS2C.STREAM_CODEC);
        PlatformNetworking.installSender(ServerPlayNetworking::send);
        CobblemonPokedexNetwork.init();
    }
}
