package com.ludwici.cobblemonpokedexnetwork.neoforge;

import com.ludwici.cobblemonpokedexnetwork.CobblemonPokedexNetwork;
import com.ludwici.cobblemonpokedexnetwork.client.ClientConfig;
import com.ludwici.cobblemonpokedexnetwork.client.CobblemonPokedexNetworkClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CobblemonPokedexNetwork.NEOFORGE_MOD_ID, dist = Dist.CLIENT)
public final class CobblemonPokedexNetworkNeoForgeClient {
    public CobblemonPokedexNetworkNeoForgeClient(ModContainer container) {
        ClientConfig.installDateFormatSupplier(NeoForgeConfig.DATE_FORMAT::get);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        CobblemonPokedexNetworkClient.init();
    }
}
