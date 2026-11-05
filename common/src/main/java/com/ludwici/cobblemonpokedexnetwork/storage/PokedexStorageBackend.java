package com.ludwici.cobblemonpokedexnetwork.storage;

import com.ludwici.cobblemonpokedexnetwork.NetworkEntry;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public interface PokedexStorageBackend extends AutoCloseable {
    Map<ResourceLocation, Map<String, NetworkEntry>> load() throws Exception;

    void save(ResourceLocation speciesId, Map<String, NetworkEntry> forms) throws Exception;

    @Override
    default void close() throws Exception {}
}
