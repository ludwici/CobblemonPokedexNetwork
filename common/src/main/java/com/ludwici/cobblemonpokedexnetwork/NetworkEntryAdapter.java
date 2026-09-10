package com.ludwici.cobblemonpokedexnetwork;

import com.cobblemon.mod.common.pokemon.Gender;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NetworkEntryAdapter extends TypeAdapter<NetworkEntry> {
    @Override
    public void write(JsonWriter out, NetworkEntry entry) throws IOException {
        if (entry == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        out.name("firstUUID").value(entry.firstUUID().toString());
        out.name("timestamp").value(entry.timestamp());
        out.name("genders").beginArray();
        for (Gender gender : entry.genders()) out.value(gender.getSerializedName());
        out.endArray();
        out.name("seenShinyStates").beginArray();
        for (String state : entry.seenShinyStates()) out.value(state);
        out.endArray();
        out.name("biomes").beginObject();
        for (var biomeEntry : entry.biomes().entrySet()) {
            out.name(biomeEntry.getKey().toString()).value(biomeEntry.getValue());
        }
        out.endObject();
        out.endObject();
    }

    @Override
    public NetworkEntry read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        UUID firstUUID = null;
        long timestamp = 0L;
        List<Gender> genders = new ArrayList<>();
        List<String> seenShinyStates = new ArrayList<>();
        Map<ResourceLocation, Integer> biomes = new HashMap<>();

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "firstUUID" -> firstUUID = UUID.fromString(in.nextString());
                case "timestamp" -> timestamp = in.nextLong();
                case "genders" -> {
                    in.beginArray();
                    while (in.hasNext()) {
                        String serialized = in.nextString();
                        Gender gender = Arrays.stream(Gender.values()).filter(value -> value.getSerializedName().equalsIgnoreCase(serialized) || value.name().equalsIgnoreCase(serialized)).findFirst().orElseThrow(() -> new IOException("Unknown gender: " + serialized));
                        genders.add(gender);
                    }
                    in.endArray();
                }
                case "seenShinyStates" -> {
                    in.beginArray();
                    while (in.hasNext()) seenShinyStates.add(in.nextString());
                    in.endArray();
                }
                case "biomes" -> {
                    in.beginObject();
                    while (in.hasNext()) {
                        biomes.put(ResourceLocation.parse(in.nextName()), in.nextInt());
                    }
                    in.endObject();
                }
                default -> in.skipValue();
            }
        }
        in.endObject();

        if (firstUUID == null) throw new IOException("Network entry is missing firstUUID");
        return new NetworkEntry(firstUUID, timestamp, genders, seenShinyStates, biomes);
    }
}
