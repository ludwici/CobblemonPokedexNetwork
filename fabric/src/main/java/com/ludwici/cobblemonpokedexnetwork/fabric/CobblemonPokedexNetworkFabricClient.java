package com.ludwici.cobblemonpokedexnetwork.fabric;

import com.ludwici.cobblemonpokedexnetwork.ClientNetworkStorage;
import com.ludwici.cobblemonpokedexnetwork.client.CobblemonPokedexNetworkClient;
import com.ludwici.cobblemonpokedexnetwork.network.PokedexChunkDataS2C;
import com.ludwici.cobblemonpokedexnetwork.network.PokedexSingleDataS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public final class CobblemonPokedexNetworkFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PokedexSingleDataS2C.ID, (payload, context) -> context.client().execute(() -> ClientNetworkStorage.INSTANCE.addNewData(ResourceLocation.parse(payload.speciesId()), payload.form(), payload.entry())));
        ClientPlayNetworking.registerGlobalReceiver(PokedexChunkDataS2C.ID, (payload, context) -> context.client().execute(() -> ClientNetworkStorage.INSTANCE.addNewData(payload.chunk())));
        CobblemonPokedexNetworkClient.init();
    }
}
