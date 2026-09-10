package com.ludwici.cobblemonpokedexnetwork.neoforge;

import com.ludwici.cobblemonpokedexnetwork.CobblemonPokedexNetwork;
import com.ludwici.cobblemonpokedexnetwork.platform.PlatformNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(CobblemonPokedexNetwork.NEOFORGE_MOD_ID)
public final class CobblemonPokedexNetworkNeoForge {
    public CobblemonPokedexNetworkNeoForge(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, NeoForgeConfig.SPEC);
        PlatformNetworking.installSender((player, payload) -> PacketDistributor.sendToPlayer(player, payload));
        modBus.addListener(NeoForgeNetworking::registerPayloads);
        CobblemonPokedexNetwork.init();
    }
}
