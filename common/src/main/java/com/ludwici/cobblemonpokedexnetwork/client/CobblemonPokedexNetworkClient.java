package com.ludwici.cobblemonpokedexnetwork.client;

import com.cobblemon.mod.common.platform.events.PlatformEvents;
import com.ludwici.cobblemonpokedexnetwork.ClientNetworkStorage;

public final class CobblemonPokedexNetworkClient {
    private static boolean initialized;

    private CobblemonPokedexNetworkClient() {}

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        ClientConfig.load();
        PlatformEvents.CLIENT_PLAYER_LOGIN.subscribe(event -> ClientNetworkStorage.INSTANCE.onLogin());
    }
}
