package com.ludwici.cobblemonpokedexnetwork;

import com.cobblemon.mod.common.pokemon.Gender;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NetworkEntry(UUID firstUUID, long timestamp, List<Gender> genders, List<String> seenShinyStates, Map<ResourceLocation, Integer> biomes) {
    private static final StreamCodec<ByteBuf, Gender> GENDER_STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(name -> Arrays.stream(Gender.values()).filter(gender -> gender.getSerializedName().equals(name)).findFirst().orElseThrow(), StringRepresentable::getSerializedName);

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkEntry> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, NetworkEntry::firstUUID, ByteBufCodecs.VAR_LONG, NetworkEntry::timestamp, GENDER_STREAM_CODEC.apply(ByteBufCodecs.list()), NetworkEntry::genders, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), NetworkEntry::seenShinyStates, ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT), NetworkEntry::biomes, NetworkEntry::new);
}
