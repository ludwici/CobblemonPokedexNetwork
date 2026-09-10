package com.ludwici.cobblemonpokedexnetwork.network;

import com.ludwici.cobblemonpokedexnetwork.ClientNetworkEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

import static com.ludwici.cobblemonpokedexnetwork.CobblemonPokedexNetwork.networkId;

public record PokedexChunkDataS2C(Map<ResourceLocation, Map<String, ClientNetworkEntry>> chunk) implements CustomPacketPayload {
    public static final Type<PokedexChunkDataS2C> ID = new Type<>(networkId("pokedex_chunk_data"));
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<String, ClientNetworkEntry>> FORMS_CODEC = ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ClientNetworkEntry.STREAM_CODEC);
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<ResourceLocation, Map<String, ClientNetworkEntry>>> CHUNK_CODEC = ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, FORMS_CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, PokedexChunkDataS2C> STREAM_CODEC = CHUNK_CODEC.map(PokedexChunkDataS2C::new, PokedexChunkDataS2C::chunk);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
