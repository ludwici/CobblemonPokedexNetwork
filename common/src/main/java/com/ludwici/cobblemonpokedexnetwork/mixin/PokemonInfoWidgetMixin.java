package com.ludwici.cobblemonpokedexnetwork.mixin;

import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexForm;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.pokedex.ScaledButton;
import com.cobblemon.mod.common.client.gui.pokedex.widgets.PokemonInfoWidget;
import com.ludwici.cobblemonpokedexnetwork.ClientNetworkEntry;
import com.ludwici.cobblemonpokedexnetwork.ClientNetworkStorage;
import com.ludwici.cobblemonpokedexnetwork.client.ClientConfig;
import com.ludwici.cobblemonpokedexnetwork.client.PokedexMultilineTooltip;
import kotlin.jvm.functions.Function1;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource;

@Mixin(PokemonInfoWidget.class)
public abstract class PokemonInfoWidgetMixin {
    @Unique private ScaledButton cpn$infoButton;
    @Unique private ClientNetworkEntry cpn$clientNetworkEntry;
    @Unique private static final ResourceLocation cpn$infoButtonResource = cobblemonResource("textures/gui/pokedex/tab_info.png");

    @Inject(method = "<init>", at = @At("TAIL"))
    private void cpn$onInit(int pX, int pY, Function1<?, ?> updateForm, CallbackInfo ci) {
        cpn$infoButton = new ScaledButton(pX + 3, pY + 41, 20, 20, cpn$infoButtonResource, 0.5F, false, button -> {});
        ((ParentWidgetInvoker) this).invokeAddWidget(cpn$infoButton);
    }

    @Inject(method = "setupButtons", at = @At("TAIL"), remap = false)
    private void cpn$setupInfo(PokedexEntry pokedexEntry, PokedexForm pokedexForm, CallbackInfo ci) {
        cpn$infoButton.visible = false;
        cpn$clientNetworkEntry = null;

        var species = PokemonSpecies.getByIdentifier(pokedexEntry.getSpeciesId());
        if (species == null) return;
        var entry = ClientNetworkStorage.INSTANCE.getInfo(species.resourceIdentifier);
        if (entry == null) return;

        var form = species.getForms().stream().filter(candidate -> candidate.getName().equalsIgnoreCase(pokedexForm.getDisplayForm())).findFirst().orElse(species.getStandardForm());
        cpn$clientNetworkEntry = entry.get(form.getName());
        if (cpn$clientNetworkEntry == null) return;

        cpn$infoButton.visible = true;
        cpn$infoButton.active = true;
    }

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void cpn$renderInfo(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        PokedexEntry current = ((PokemonInfoWidgetAccessor) this).getSelectedEntry();
        if (current == null) return;

        boolean hasKnowledge = CobblemonClient.INSTANCE.getClientPokedexData().getKnowledgeForSpecies(current.getSpeciesId()) != PokedexEntryProgress.UNREGISTERED;
        if (!hasKnowledge || cpn$clientNetworkEntry == null) return;

        cpn$infoButton.render(context, mouseX, mouseY, delta);
        if (cpn$infoButton.isButtonHovered(mouseX, mouseY)) {
            PokedexMultilineTooltip.render(context, Component.translatable("cobblemon-pokedex-network.gui.caught_info", cpn$clientNetworkEntry.firstUserName(), cpn$clientNetworkEntry.getFormattedData(ClientConfig.dateFormat())), mouseX, mouseY, delta, 10);
        }
    }
}
