package com.ludwici.cobblemonpokedexnetwork;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientNetworkStorage {
    public static final ClientNetworkStorage INSTANCE = new ClientNetworkStorage();
    private Map<ResourceLocation, Map<String, ClientNetworkEntry>> data = new ConcurrentHashMap<>();

    private ClientNetworkStorage() {}

    public void onLogin() {
        data = new ConcurrentHashMap<>();
    }

    public void addNewData(ResourceLocation speciesId, String form, ClientNetworkEntry networkEntry) {
        data.computeIfAbsent(speciesId, key -> new ConcurrentHashMap<>()).put(form, networkEntry);
    }

    public void addNewData(Map<ResourceLocation, Map<String, ClientNetworkEntry>> newData) {
        data.putAll(newData);
    }

    public Map<String, ClientNetworkEntry> getInfo(ResourceLocation speciesId) {
        return data.get(speciesId);
    }
}
