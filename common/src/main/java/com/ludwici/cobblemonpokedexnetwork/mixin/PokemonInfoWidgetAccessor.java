package com.ludwici.cobblemonpokedexnetwork.mixin;

import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.cobblemon.mod.common.client.gui.pokedex.widgets.PokemonInfoWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PokemonInfoWidget.class)
public interface PokemonInfoWidgetAccessor {
    @Accessor("currentEntry")
    PokedexEntry getSelectedEntry();
}
