package com.ludwici.cobblemonpokedexnetwork.network;

import com.ludwici.cobblemonpokedexnetwork.ClientNetworkEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static com.ludwici.cobblemonpokedexnetwork.CobblemonPokedexNetwork.networkId;

public record PokedexSingleDataS2C(String speciesId, String form, ClientNetworkEntry entry) implements CustomPacketPayload {
    public static final Type<PokedexSingleDataS2C> ID = new Type<>(networkId("pokedex_single_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PokedexSingleDataS2C> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PokedexSingleDataS2C::speciesId, ByteBufCodecs.STRING_UTF8, PokedexSingleDataS2C::form, ClientNetworkEntry.STREAM_CODEC, PokedexSingleDataS2C::entry, PokedexSingleDataS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
