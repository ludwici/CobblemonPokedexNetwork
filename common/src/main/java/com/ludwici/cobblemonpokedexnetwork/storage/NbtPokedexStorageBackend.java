package com.ludwici.cobblemonpokedexnetwork.storage;

import com.cobblemon.mod.common.pokemon.Gender;
import com.ludwici.cobblemonpokedexnetwork.NetworkEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public final class NbtPokedexStorageBackend implements PokedexStorageBackend {
    private final Path folder;

    public NbtPokedexStorageBackend(Path folder) throws IOException {
        this.folder = folder;
        Files.createDirectories(folder);
    }

    @Override
    public Map<ResourceLocation, Map<String, NetworkEntry>> load() throws IOException {
        Map<ResourceLocation, Map<String, NetworkEntry>> result = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path path : files.filter(file -> file.getFileName().toString().endsWith(".nbt")).toList()) {
                try {
                    CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.create(32L * 1024L * 1024L));
                    ResourceLocation speciesId = ResourceLocation.parse(root.getString("species"));
                    if (!path.getFileName().toString().equals(toFileName(speciesId))) {
                        throw new IOException("CPN NBT filename does not match species: " + path);
                    }

                    Map<String, NetworkEntry> forms = new LinkedHashMap<>();
                    ListTag entries = root.getList("forms", Tag.TAG_COMPOUND);
                    for (int i = 0; i < entries.size(); i++) {
                        CompoundTag formTag = entries.getCompound(i);
                        String name = formTag.getString("name");
                        UUID firstUUID = UUID.fromString(formTag.getString("firstUUID"));
                        long timestamp = formTag.getLong("timestamp");
                        List<Gender> genders = new ArrayList<>();
                        ListTag genderList = formTag.getList("genders", Tag.TAG_STRING);
                        for (int j = 0; j < genderList.size(); j++) {
                            String genderName = genderList.getString(j);
                            Gender gender = Arrays.stream(Gender.values()).filter(value -> value.getSerializedName().equalsIgnoreCase(genderName) || value.name().equalsIgnoreCase(genderName)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown gender: " + genderName));
                            genders.add(gender);
                        }

                        List<String> shinyStates = new ArrayList<>();
                        ListTag shinyList = formTag.getList("seenShinyStates", Tag.TAG_STRING);
                        for (int j = 0; j < shinyList.size(); j++) {
                            shinyStates.add(shinyList.getString(j));
                        }

                        Map<ResourceLocation, Integer> biomes = new HashMap<>();
                        ListTag biomeList = formTag.getList("biomes", Tag.TAG_COMPOUND);
                        for (int j = 0; j < biomeList.size(); j++) {
                            CompoundTag biomeTag = biomeList.getCompound(j);
                            biomes.put(ResourceLocation.parse(biomeTag.getString("id")), biomeTag.getInt("count"));
                        }

                        if (forms.putIfAbsent(name, new NetworkEntry(firstUUID, timestamp, List.copyOf(genders), List.copyOf(shinyStates), Map.copyOf(biomes))) != null) {
                            throw new IOException("Duplicate CPN form in " + path + ": " + name);
                        }
                    }
                    if (result.putIfAbsent(speciesId, forms) != null) {
                        throw new IOException("Duplicate CPN species: " + speciesId);
                    }
                } catch (RuntimeException e) {
                    throw new IOException("Could not load CPN NBT entry: " + path, e);
                }
            }
        }
        return result;
    }

    @Override
    public void save(ResourceLocation speciesId, Map<String, NetworkEntry> forms) throws IOException {
        CompoundTag root = new CompoundTag();
        root.putString("species", speciesId.toString());
        ListTag entries = new ListTag();
        for (Map.Entry<String, NetworkEntry> form : forms.entrySet()) {
            NetworkEntry entry = form.getValue();
            CompoundTag formTag = new CompoundTag();
            formTag.putString("name", form.getKey());
            formTag.putString("firstUUID", entry.firstUUID().toString());
            formTag.putLong("timestamp", entry.timestamp());

            ListTag genders = new ListTag();
            for (Gender gender : entry.genders()) {
                genders.add(StringTag.valueOf(gender.getSerializedName()));
            }
            formTag.put("genders", genders);

            ListTag shiny = new ListTag();
            for (String state : entry.seenShinyStates()) {
                shiny.add(StringTag.valueOf(state));
            }
            formTag.put("seenShinyStates", shiny);

            ListTag biomes = new ListTag();
            for (Map.Entry<ResourceLocation, Integer> biome : entry.biomes().entrySet()) {
                CompoundTag biomeTag = new CompoundTag();
                biomeTag.putString("id", biome.getKey().toString());
                biomeTag.putInt("count", biome.getValue());
                biomes.add(biomeTag);
            }
            formTag.put("biomes", biomes);
            entries.add(formTag);
        }
        root.put("forms", entries);

        Path temporary = Files.createTempFile(folder, "cpn-", ".tmp");
        try {
            NbtIo.writeCompressed(root, temporary);
            Path destination = folder.resolve(toFileName(speciesId));
            if (Files.exists(destination)) {
                Files.copy(destination, destination.resolveSibling(destination.getFileName().toString() + ".old"), StandardCopyOption.REPLACE_EXISTING);
            }
            JsonPokedexStorageBackend.moveSafely(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String toFileName(ResourceLocation id) {
        return "p_" + Base64.getUrlEncoder().withoutPadding().encodeToString(id.toString().getBytes(StandardCharsets.UTF_8)) + ".nbt";
    }
}
