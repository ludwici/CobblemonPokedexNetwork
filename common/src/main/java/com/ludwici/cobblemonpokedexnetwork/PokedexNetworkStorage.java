package com.ludwici.cobblemonpokedexnetwork;

import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ludwici.cobblemonpokedexnetwork.network.PokedexChunkDataS2C;
import com.ludwici.cobblemonpokedexnetwork.network.PokedexSingleDataS2C;
import com.ludwici.cobblemonpokedexnetwork.platform.PlatformNetworking;
import com.ludwici.cobblemonpokedexnetwork.storage.JsonPokedexStorageBackend;
import com.ludwici.cobblemonpokedexnetwork.storage.PokedexStorageBackend;
import com.ludwici.cobblemonpokedexnetwork.storage.PokedexStorageBackends;
import com.ludwici.cobblemonpokedexnetwork.storage.PokedexStorageConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.TimeUnit;

public final class PokedexNetworkStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("pokenet");

    private final MinecraftServer server;
    private final PokedexStorageBackend backend;
    private final Map<ResourceLocation, Map<String, NetworkEntry>> data = new ConcurrentHashMap<>();
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("cpn-storage-%d").build());
    private final Map<ResourceLocation, Map<String, NetworkEntry>> failedSaves = new ConcurrentHashMap<>();
    private boolean closed;

    public PokedexNetworkStorage(MinecraftServer server) {
        this.server = server;
        PokedexStorageBackend opened = null;
        try {
            PokedexStorageConfig config = PokedexStorageConfig.load();
            verifySelectedBackend(config);
            opened = PokedexStorageBackends.create(config.storageFormat, server, config);
            Map<ResourceLocation, Map<String, NetworkEntry>> loaded = opened.load();
            migrateIfRequested(config, opened, loaded);
            ensureNoUnmigratedFileData(config, loaded);
            recordSelectedBackend(config.storageFormat);
            for (Map.Entry<ResourceLocation, Map<String, NetworkEntry>> species : loaded.entrySet()) {
                data.put(species.getKey(), new ConcurrentHashMap<>(species.getValue()));
            }
            LOGGER.info("CPN storage initialized: format={}, species={}", config.storageFormat, data.size());
        } catch (Exception e) {
            if (opened != null) {
                try {
                    opened.close();
                } catch (Exception closeException) {
                    e.addSuppressed(closeException);
                }
            }
            throw new IllegalStateException("Could not initialize CPN storage. Existing data was not replaced; check the storage configuration and server log", e);
        }
        this.backend = opened;
    }

    private void verifySelectedBackend(PokedexStorageConfig config) throws IOException {
        Path state = server.getWorldPath(LevelResource.ROOT).resolve("pokenet_storage_state.txt");
        if (!Files.exists(state)) {
            return;
        }
        String previous = Files.readString(state, StandardCharsets.UTF_8).trim();
        if (!previous.equals(config.storageFormat) && !previous.equals(config.migrationSourceFormat)) {
            throw new IOException("CPN was last started with storageFormat=" + previous + ". Set migrationSourceFormat=" + previous + " when changing to " + config.storageFormat);
        }
    }

    private void recordSelectedBackend(String format) throws IOException {
        Path state = server.getWorldPath(LevelResource.ROOT).resolve("pokenet_storage_state.txt");
        if (Files.exists(state) && Files.readString(state, StandardCharsets.UTF_8).trim().equals(format)) {
            return;
        }
        Path temporary = Files.createTempFile(state.getParent(), "cpn-state-", ".tmp");
        try {
            Files.writeString(temporary, format, StandardCharsets.UTF_8);
            JsonPokedexStorageBackend.moveSafely(temporary, state);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void migrateIfRequested(PokedexStorageConfig config, PokedexStorageBackend destination, Map<ResourceLocation, Map<String, NetworkEntry>> loaded) throws Exception {
        if (config.migrationSourceFormat.isEmpty()) {
            return;
        }
        Path migrationFolder = server.getWorldPath(LevelResource.ROOT).resolve("pokenet_migrations");
        Path marker = migrationFolder.resolve(config.migrationSourceFormat + "-to-" + config.storageFormat + ".done");
        if (Files.exists(marker)) {
            Path state = server.getWorldPath(LevelResource.ROOT).resolve("pokenet_storage_state.txt");
            if (Files.exists(state) && Files.readString(state, StandardCharsets.UTF_8).trim().equals(config.storageFormat)) {
                LOGGER.info("CPN storage migration {} -> {} has already been completed", config.migrationSourceFormat, config.storageFormat);
                return;
            }
            throw new IOException("CPN migration " + config.migrationSourceFormat + " -> " + config.storageFormat + " was already completed earlier. Back up all storage and review/remove the old migration marker before attempting a second migration of the same format pair");
        }

        try (PokedexStorageBackend source = PokedexStorageBackends.create(config.migrationSourceFormat, server, config)) {
            Map<ResourceLocation, Map<String, NetworkEntry>> sourceData = source.load();
            if (sourceData.isEmpty()) {
                throw new IOException("CPN migration source is empty; refusing to mark migration as completed");
            }
            for (Map.Entry<ResourceLocation, Map<String, NetworkEntry>> existing : loaded.entrySet()) {
                if (!sourceData.containsKey(existing.getKey())) {
                    throw new IOException("Destination contains CPN species absent from the migration source: " + existing.getKey());
                }
                if (!existing.getValue().equals(sourceData.get(existing.getKey()))) {
                    throw new IOException("Conflicting CPN migration data for species " + existing.getKey());
                }
            }
            int imported = 0;
            for (Map.Entry<ResourceLocation, Map<String, NetworkEntry>> species : sourceData.entrySet()) {
                if (!loaded.containsKey(species.getKey())) {
                    destination.save(species.getKey(), Map.copyOf(species.getValue()));
                    loaded.put(species.getKey(), species.getValue());
                    imported++;
                }
            }
            Files.createDirectories(migrationFolder);
            Path temporaryMarker = Files.createTempFile(migrationFolder, "cpn-migration-", ".tmp");
            try {
                Files.writeString(temporaryMarker, "source=" + config.migrationSourceFormat + "\ntarget=" + config.storageFormat + "\n", StandardCharsets.UTF_8);
                JsonPokedexStorageBackend.moveSafely(temporaryMarker, marker);
            } finally {
                Files.deleteIfExists(temporaryMarker);
            }
            LOGGER.info("Completed CPN migration {} -> {}, imported {} species", config.migrationSourceFormat, config.storageFormat, imported);
        }
    }

    private void ensureNoUnmigratedFileData(PokedexStorageConfig config, Map<ResourceLocation, Map<String, NetworkEntry>> loaded) throws IOException {
        if (!config.migrationSourceFormat.isEmpty() || !loaded.isEmpty()) {
            return;
        }
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        for (String format : List.of("json", "nbt")) {
            if (config.storageFormat.equals(format)) {
                continue;
            }
            Path previousFolder = worldRoot.resolve(format.equals("json") ? "pokenet" : "pokenet_nbt");
            String suffix = format.equals("json") ? ".json" : ".nbt";
            if (Files.isDirectory(previousFolder)) {
                try (var entries = Files.list(previousFolder)) {
                    if (entries.anyMatch(path -> path.getFileName().toString().endsWith(suffix))) {
                        throw new IOException("Found existing CPN " + format + " data in " + previousFolder + ". Set migrationSourceFormat to " + format + " before switching storageFormat");
                    }
                }
            }
        }
    }

    public Map<String, NetworkEntry> getInfo(ResourceLocation speciesId) {
        return data.get(speciesId);
    }

    public void addNewData(ResourceLocation speciesId, UUID playerUUID, Pokemon pokemon, ResourceLocation biome) {
        if (closed) {
            return;
        }
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
            if (!genders.contains(gender)) {
                genders.add(gender);
            }
            List<String> shinyStates = new ArrayList<>(current.seenShinyStates());
            if (!shinyStates.contains(shiny)) {
                shinyStates.add(shiny);
            }
            return new NetworkEntry(current.firstUUID(), current.timestamp(), List.copyOf(genders), List.copyOf(shinyStates), Map.copyOf(biomes));
        });

        Map<String, NetworkEntry> snapshot = Map.copyOf(forms);
        saveExecutor.execute(() -> {
            try {
                backend.save(speciesId, snapshot);
                failedSaves.remove(speciesId);
            } catch (Exception e) {
                failedSaves.put(speciesId, snapshot);
                LOGGER.error("Failed to persist CPN data for {}", speciesId, e);
            }
        });
    }

    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        saveExecutor.shutdown();
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    if (saveExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        break;
                    }
                    LOGGER.warn("Waiting for pending CPN storage writes to complete");
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            for (Map.Entry<ResourceLocation, Map<String, NetworkEntry>> pending : failedSaves.entrySet()) {
                try {
                    backend.save(pending.getKey(), pending.getValue());
                    failedSaves.remove(pending.getKey());
                } catch (Exception e) {
                    LOGGER.error("Final CPN save retry failed for {}", pending.getKey(), e);
                }
            }
            backend.close();
        } catch (Exception e) {
            LOGGER.error("Failed to close CPN storage backend", e);
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
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
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }
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

    public void toClientAllPlayers(ResourceLocation speciesId, FormData formData) {
        Map<String, NetworkEntry> forms = getInfo(speciesId);
        if (forms == null) {
            return;
        }
        NetworkEntry entry = forms.get(formData.getName());
        if (entry == null) {
            return;
        }
        PokedexSingleDataS2C payload = new PokedexSingleDataS2C(speciesId.toString(), formData.getName(), toClientEntry(entry));
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            PlatformNetworking.sendToPlayer(onlinePlayer, payload);
        }
    }

    public void toClientSingleData(ServerPlayer player, ResourceLocation speciesId, FormData formData) {
        Map<String, NetworkEntry> forms = getInfo(speciesId);
        if (forms == null) {
            return;
        }
        NetworkEntry entryData = forms.get(formData.getName());
        if (entryData == null) {
            return;
        }
        PlatformNetworking.sendToPlayer(player, new PokedexSingleDataS2C(speciesId.toString(), formData.getName(), toClientEntry(entryData)));
    }
}
