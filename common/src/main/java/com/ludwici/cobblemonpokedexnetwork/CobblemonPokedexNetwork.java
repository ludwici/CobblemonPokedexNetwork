package com.ludwici.cobblemonpokedexnetwork;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.platform.events.PlatformEvents;
import com.cobblemon.mod.common.pokedex.scanner.PokedexEntityData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CobblemonPokedexNetwork {
    public static final String NEOFORGE_MOD_ID = "cobblemonpokedexnetwork";
    public static final String RESOURCE_NAMESPACE = "cobblemon-pokedex-network";
    public static final Logger LOGGER = LoggerFactory.getLogger(RESOURCE_NAMESPACE);

    private static PokedexNetworkStorage pokedexStorage;
    private static boolean initialized;

    private CobblemonPokedexNetwork() {}

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        PlatformEvents.SERVER_STARTING.subscribe(event -> pokedexStorage = new PokedexNetworkStorage(event.getServer()));

        PlatformEvents.SERVER_PLAYER_LOGIN.subscribe(event -> {
            if (pokedexStorage != null) {
                pokedexStorage.toClientAllData(event.getPlayer());
            }
        });

        CobblemonEvents.POKEMON_SCANNED.subscribe(event -> {
            if (pokedexStorage == null) return;

            PokedexEntityData entityData = event.getScannedPokemonEntityData();
            if (entityData.getDisguise() != null) return;

            Pokemon pokemon = entityData.getPokemon();
            ResourceLocation speciesId = pokemon.getSpecies().resourceIdentifier;
            var formData = pokedexStorage.getInfo(speciesId);
            if (formData == null) return;

            NetworkEntry entry = formData.get(pokemon.getForm().getName());
            if (entry == null) return;

            PokedexManager dex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(event.getPlayer());
            for (Gender gender : entry.genders()) {
                for (String shinyState : entry.seenShinyStates()) {
                    Pokemon sharedKnowledge = new Pokemon();
                    sharedKnowledge.setSpecies(pokemon.getSpecies());
                    sharedKnowledge.setGender(gender);
                    sharedKnowledge.setShiny("shiny".equals(shinyState));
                    sharedKnowledge.setForm(pokemon.getForm());
                    dex.obtain(sharedKnowledge);
                }
            }
        });

        CobblemonEvents.POKEMON_CAPTURED.subscribe(event -> {
            if (pokedexStorage == null) return;

            ServerPlayer player = event.getPlayer();
            Pokemon pokemon = event.getPokemon();
            ResourceLocation biome = player.level().getBiome(player.blockPosition()).unwrapKey().map(net.minecraft.resources.ResourceKey::location).orElse(null);
            if (biome == null) return;

            ResourceLocation speciesId = pokemon.getSpecies().resourceIdentifier;
            pokedexStorage.addNewData(speciesId, player.getUUID(), pokemon, biome);
            pokedexStorage.toClientSingleData(player, speciesId, pokemon.getForm());
        });
    }

    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(RESOURCE_NAMESPACE, path);
    }

    public static ResourceLocation networkId(String path) {
        return ResourceLocation.fromNamespaceAndPath(RESOURCE_NAMESPACE, path);
    }
}
