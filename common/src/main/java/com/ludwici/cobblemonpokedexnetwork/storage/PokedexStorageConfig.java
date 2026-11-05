package com.ludwici.cobblemonpokedexnetwork.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class PokedexStorageConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> FORMATS = Set.of("json", "nbt", "mongodb");
    private static final String FILE_NAME = "cobblemon-pokedex-network-storage.json";

    public String storageFormat = "json";
    public String mongoDBConnectionString = "mongodb://localhost:27017";
    public String mongoDBDatabaseName = "cobblemon";
    public String mongoDBCollectionName = "PokedexNetwork";
    public String migrationSourceFormat = "";

    private PokedexStorageConfig() {}

    public static PokedexStorageConfig load() throws IOException {
        Path path = Platform.getConfigFolder().resolve(FILE_NAME);
        Files.createDirectories(path.getParent());
        if (!Files.exists(path)) {
            PokedexStorageConfig config = new PokedexStorageConfig();
            Files.writeString(path, GSON.toJson(config), StandardCharsets.UTF_8);
            return config;
        }

        PokedexStorageConfig config;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            config = GSON.fromJson(reader, PokedexStorageConfig.class);
        } catch (RuntimeException e) {
            throw new IOException("Invalid CPN storage configuration: " + path, e);
        }
        if (config == null) {
            throw new IOException("Empty CPN storage configuration: " + path);
        }

        config.storageFormat = normalizeFormat(config.storageFormat);
        config.migrationSourceFormat = config.migrationSourceFormat == null || config.migrationSourceFormat.isBlank() ? "" : normalizeFormat(config.migrationSourceFormat);
        if (!config.migrationSourceFormat.isEmpty() && config.storageFormat.equals(config.migrationSourceFormat)) {
            throw new IOException("migrationSourceFormat must differ from storageFormat");
        }
        if (config.mongoDBConnectionString == null || config.mongoDBConnectionString.isBlank()) {
            throw new IOException("mongoDBConnectionString must not be empty");
        }
        if (config.mongoDBDatabaseName == null || config.mongoDBDatabaseName.isBlank()) {
            throw new IOException("mongoDBDatabaseName must not be empty");
        }
        if (config.mongoDBCollectionName == null || config.mongoDBCollectionName.isBlank()) {
            throw new IOException("mongoDBCollectionName must not be empty");
        }
        return config;
    }

    private static String normalizeFormat(String value) throws IOException {
        String format = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!FORMATS.contains(format)) {
            throw new IOException("Unsupported CPN storageFormat: " + value + ". Expected json, nbt or mongodb");
        }
        return format;
    }
}
