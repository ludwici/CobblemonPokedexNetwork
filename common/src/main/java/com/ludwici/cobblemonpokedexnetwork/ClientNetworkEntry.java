package com.ludwici.cobblemonpokedexnetwork;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public record ClientNetworkEntry(String firstUserName, long timestamp, Map<ResourceLocation, Integer> biomes) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientNetworkEntry> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ClientNetworkEntry::firstUserName, ByteBufCodecs.VAR_LONG, ClientNetworkEntry::timestamp, ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT), ClientNetworkEntry::biomes, ClientNetworkEntry::new);

    public static ClientNetworkEntry from(String name, NetworkEntry networkEntry) {
        return new ClientNetworkEntry(name, networkEntry.timestamp(), networkEntry.biomes());
    }

    public String getFormattedData(String pattern) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern));
    }
}
