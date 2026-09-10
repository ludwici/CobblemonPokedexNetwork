package com.ludwici.cobblemonpokedexnetwork.neoforge;

import com.ludwici.cobblemonpokedexnetwork.ClientNetworkStorage;
import com.ludwici.cobblemonpokedexnetwork.CobblemonPokedexNetwork;
import com.ludwici.cobblemonpokedexnetwork.network.PokedexChunkDataS2C;
import com.ludwici.cobblemonpokedexnetwork.network.PokedexSingleDataS2C;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgeNetworking {
    private NeoForgeNetworking() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CobblemonPokedexNetwork.NEOFORGE_MOD_ID).versioned("1.0.0");

        registrar.playToClient(PokedexSingleDataS2C.ID, PokedexSingleDataS2C.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientNetworkStorage.INSTANCE.addNewData(ResourceLocation.parse(payload.speciesId()), payload.form(), payload.entry())));

        registrar.playToClient(PokedexChunkDataS2C.ID, PokedexChunkDataS2C.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientNetworkStorage.INSTANCE.addNewData(payload.chunk())));
    }
}
