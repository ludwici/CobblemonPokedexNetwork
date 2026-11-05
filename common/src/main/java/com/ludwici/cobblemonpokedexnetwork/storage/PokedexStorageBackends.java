package com.ludwici.cobblemonpokedexnetwork.storage;

import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public final class PokedexStorageBackends {
    private PokedexStorageBackends() {}

    public static PokedexStorageBackend create(String format, MinecraftServer server, PokedexStorageConfig config) throws Exception {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        return switch (format) {
            case "json" -> new JsonPokedexStorageBackend(worldRoot.resolve("pokenet"));
            case "nbt" -> new NbtPokedexStorageBackend(worldRoot.resolve("pokenet_nbt"));
            case "mongodb" -> new MongoPokedexStorageBackend(config);
            default -> throw new IllegalArgumentException("Unsupported CPN storage format: " + format);
        };
    }
}
