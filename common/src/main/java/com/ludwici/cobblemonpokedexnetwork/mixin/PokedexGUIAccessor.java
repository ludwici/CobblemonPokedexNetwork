package com.ludwici.cobblemonpokedexnetwork.mixin;

import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexForm;
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUI;
import com.cobblemon.mod.common.client.gui.pokedex.ScaledButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PokedexGUI.class)
public interface PokedexGUIAccessor {
    @Accessor("selectedEntry") PokedexEntry getSelectedEntry();
    @Accessor("selectedForm") PokedexForm getSelectedForm();
    @Accessor("tabButtons") List<ScaledButton> getTabButtons();
}
