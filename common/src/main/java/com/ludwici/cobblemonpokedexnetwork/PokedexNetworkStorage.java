package com.ludwici.cobblemonpokedexnetwork;

import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.ludwici.cobblemonpokedexnetwork.network.PokedexChunkDataS2C;
import com.ludwici.cobblemonpokedexnetwork.network.PokedexSingleDataS2C;
import com.ludwici.cobblemonpokedexnetwork.platform.PlatformNetworking;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public final class PokedexNetworkStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("pokenet");
    private static final String FOLDER_NAME = "pokenet";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(NetworkEntry.class, new NetworkEntryAdapter()).create();
    private static final Type FORMS_TYPE = new TypeToken<Map<String, NetworkEntry>>() {}.getType();
    @SuppressWarnings("unchecked")
    private static final TypeAdapter<Map<String, NetworkEntry>> FORMS_ADAPTER = (TypeAdapter<Map<String, NetworkEntry>>) GSON.getAdapter(TypeToken.get(FORMS_TYPE));

    private final MinecraftServer server;
    private final Path filePath;
    private final Map<ResourceLocation, Map<String, NetworkEntry>> data = new ConcurrentHashMap<>();
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("cpn-storage-%d").build());

    public PokedexNetworkStorage(MinecraftServer server) {
        this.server = server;
        this.filePath = server.getWorldPath(LevelResource.ROOT).resolve(FOLDER_NAME);
        try {
            Files.createDirectories(filePath);
        } catch (IOException e) {
            LOGGER.error("Could not create storage directory {}", filePath, e);
        }
        load();
    }

    public Map<String, NetworkEntry> getInfo(ResourceLocation speciesId) {
        return data.get(speciesId);
    }

    public void addNewData(ResourceLocation speciesId, UUID playerUUID, Pokemon pokemon, ResourceLocation biome) {
        String formName = pokemon.getForm().getName();
        Gender gender = pokemon.getGender();
        String shiny = pokemon.getShiny() ? "shiny" : "normal";
        Map<String, NetworkEntry> forms = data.computeIfAbsent(speciesId, key -> new ConcurrentHashMap<>());

        forms.compute(formName, (key, current) -> {
            if (current == null) {
                return new NetworkEntry(playerUUID, System.currentTimeMillis(), List.of(gender), List.of(shiny), Map.of(biome, 1));
            }
            Map<ResourceLocation, Integer> biomes = new HashMap<>(current.biomes());
            biomes.merge(biome, 1, Integer::sum);
            List<Gender> genders = new ArrayList<>(current.genders());
            if (!genders.contains(gender)) genders.add(gender);
            List<String> shinyStates = new ArrayList<>(current.seenShinyStates());
            if (!shinyStates.contains(shiny)) shinyStates.add(shiny);
            return new NetworkEntry(current.firstUUID(), current.timestamp(), List.copyOf(genders), List.copyOf(shinyStates), Map.copyOf(biomes));
        });

        Map<String, NetworkEntry> snapshot = Map.copyOf(forms);
        saveExecutor.execute(() -> save(speciesId, snapshot));
    }

    private void save(ResourceLocation speciesId, Map<String, NetworkEntry> forms) {
        if (forms.isEmpty()) return;
        Path file = filePath.resolve(toFileName(speciesId));
        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(file))) {
            FORMS_ADAPTER.write(writer, forms);
        } catch (IOException e) {
            LOGGER.error("Failed to save {}", file, e);
        }
    }

    private void load() {
        if (!Files.exists(filePath)) return;
        try (Stream<Path> files = Files.list(filePath)) {
            files.filter(path -> path.toString().endsWith(".json")).filter(path -> path.toFile().length() > 0L).forEach(this::loadFile);
        } catch (IOException e) {
            LOGGER.error("Failed to read storage directory {}", filePath, e);
        }
    }

    private void loadFile(Path path) {
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(path))) {
            Map<String, NetworkEntry> forms = FORMS_ADAPTER.read(reader);
            if (forms != null) {
                data.put(fileNameToPokemon(path), new ConcurrentHashMap<>(forms));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read {}", path, e);
        }
    }

    private static String toFileName(ResourceLocation id) {
        return id.getNamespace() + "_" + id.getPath() + ".json";
    }

    private static ResourceLocation fileNameToPokemon(Path path) {
        String file = path.getFileName().toString();
        file = file.substring(0, file.length() - 5);
        int index = file.indexOf('_');
        if (index <= 0 || index == file.length() - 1) {
            throw new IllegalArgumentException("Invalid pokenet filename: " + path.getFileName());
        }
        return ResourceLocation.fromNamespaceAndPath(file.substring(0, index), file.substring(index + 1));
    }

    private static <K, V> List<Map<K, V>> chunkMap(Map<K, V> map, int chunkSize) {
        List<Map<K, V>> chunks = new ArrayList<>();
        Map<K, V> currentChunk = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            currentChunk.put(entry.getKey(), entry.getValue());
            if (currentChunk.size() == chunkSize) {
                chunks.add(currentChunk);
                currentChunk = new LinkedHashMap<>();
            }
        }
        if (!currentChunk.isEmpty()) chunks.add(currentChunk);
        return chunks;
    }

    private Map<ResourceLocation, Map<String, ClientNetworkEntry>> convertToClientData() {
        Map<ResourceLocation, Map<String, ClientNetworkEntry>> result = new LinkedHashMap<>();
        for (var speciesEntry : data.entrySet()) {
            Map<String, ClientNetworkEntry> clientForms = new LinkedHashMap<>();
            for (var formEntry : speciesEntry.getValue().entrySet()) {
                clientForms.put(formEntry.getKey(), toClientEntry(formEntry.getValue()));
            }
            result.put(speciesEntry.getKey(), clientForms);
        }
        return result;
    }

    private ClientNetworkEntry toClientEntry(NetworkEntry entry) {
        Optional<GameProfile> profile = server.getProfileCache().get(entry.firstUUID());
        String name = profile.map(GameProfile::getName).orElse(entry.firstUUID().toString());
        return ClientNetworkEntry.from(name, entry);
    }

    public void toClientAllData(ServerPlayer player) {
        for (Map<ResourceLocation, Map<String, ClientNetworkEntry>> chunk : chunkMap(convertToClientData(), 100)) {
            PlatformNetworking.sendToPlayer(player, new PokedexChunkDataS2C(chunk));
        }
    }

    public void toClientSingleData(ServerPlayer player, ResourceLocation speciesId, FormData formData) {
        Map<String, NetworkEntry> forms = getInfo(speciesId);
        if (forms == null) return;
        NetworkEntry entryData = forms.get(formData.getName());
        if (entryData == null) return;
        PlatformNetworking.sendToPlayer(player, new PokedexSingleDataS2C(speciesId.toString(), formData.getName(), toClientEntry(entryData)));
    }
}
