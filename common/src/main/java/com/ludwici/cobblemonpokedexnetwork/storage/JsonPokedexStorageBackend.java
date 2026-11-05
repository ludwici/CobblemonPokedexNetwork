package com.ludwici.cobblemonpokedexnetwork.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ludwici.cobblemonpokedexnetwork.NetworkEntry;
import com.ludwici.cobblemonpokedexnetwork.NetworkEntryAdapter;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class JsonPokedexStorageBackend implements PokedexStorageBackend {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(NetworkEntry.class, new NetworkEntryAdapter()).create();
    private static final Type FORMS_TYPE = new TypeToken<Map<String, NetworkEntry>>() {}.getType();
    private final Path folder;

    public JsonPokedexStorageBackend(Path folder) throws IOException {
        this.folder = folder;
        Files.createDirectories(folder);
    }

    public static String serialize(Map<String, NetworkEntry> forms) {
        return GSON.toJson(forms, FORMS_TYPE);
    }

    public static Map<String, NetworkEntry> deserialize(String json) throws IOException {
        try {
            Map<String, NetworkEntry> forms = GSON.fromJson(json, FORMS_TYPE);
            if (forms == null) {
                throw new IOException("CPN storage entry is empty");
            }
            return forms;
        } catch (RuntimeException e) {
            throw new IOException("Invalid CPN JSON storage entry", e);
        }
    }

    @Override
    public Map<ResourceLocation, Map<String, NetworkEntry>> load() throws IOException {
        Map<ResourceLocation, Map<String, NetworkEntry>> result = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path path : files.filter(file -> file.getFileName().toString().endsWith(".json")).toList()) {
                ResourceLocation speciesId = fromFileName(path);
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    Map<String, NetworkEntry> forms = GSON.fromJson(reader, FORMS_TYPE);
                    if (forms == null) {
                        throw new IOException("Empty CPN entry: " + path);
                    }
                    if (result.putIfAbsent(speciesId, forms) != null) {
                        throw new IOException("Duplicate CPN species: " + speciesId);
                    }
                } catch (RuntimeException e) {
                    throw new IOException("Could not load CPN entry: " + path, e);
                }
            }
        }
        return result;
    }

    @Override
    public void save(ResourceLocation speciesId, Map<String, NetworkEntry> forms) throws IOException {
        Path destination = folder.resolve(toFileName(speciesId));
        Path temporary = Files.createTempFile(folder, "cpn-", ".tmp");
        try {
            Files.writeString(temporary, serialize(forms), StandardCharsets.UTF_8);
            if (Files.exists(destination)) {
                Files.copy(destination, destination.resolveSibling(destination.getFileName().toString() + ".old"), StandardCopyOption.REPLACE_EXISTING);
            }
            moveSafely(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void moveSafely(Path temporary, Path destination) throws IOException {
        try {
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String toFileName(ResourceLocation id) {
        return id.getNamespace() + "_" + id.getPath() + ".json";
    }

    private static ResourceLocation fromFileName(Path path) throws IOException {
        String file = path.getFileName().toString();
        String value = file.substring(0, file.length() - ".json".length());
        int index = value.indexOf('_');
        if (index <= 0 || index == value.length() - 1) {
            throw new IOException("Invalid CPN JSON filename: " + file);
        }
        try {
            return ResourceLocation.fromNamespaceAndPath(value.substring(0, index), value.substring(index + 1));
        } catch (RuntimeException e) {
            throw new IOException("Invalid CPN species identifier in " + file, e);
        }
    }
}
