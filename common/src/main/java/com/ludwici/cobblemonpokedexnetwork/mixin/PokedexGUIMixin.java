package com.ludwici.cobblemonpokedexnetwork.mixin;

import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUI;
import com.cobblemon.mod.common.client.gui.pokedex.ScaledButton;
import com.ludwici.cobblemonpokedexnetwork.ClientNetworkEntry;
import com.ludwici.cobblemonpokedexnetwork.ClientNetworkStorage;
import com.ludwici.cobblemonpokedexnetwork.client.gui.pokedex.HabitatsWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.stream.Collectors;

import static com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.BASE_HEIGHT;
import static com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.BASE_WIDTH;
import static com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.TAB_ICON_SIZE;
import static com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource;

@Mixin(PokedexGUI.class)
public abstract class PokedexGUIMixin extends Screen {
    @Shadow public GuiEventListener tabInfoElement;
    @Shadow private int tabInfoIndex;

    @Unique private int cpn$habitatsTabIndex = -1;
    @Unique private static final ResourceLocation cpn$habitatsTabIcon = cobblemonResource("textures/gui/pokedex/tab_locations.png");
    @Unique private static final float cpn$tabSpacing = 21.0F;
    @Unique private static final float cpn$tabRowShift = 8.0F;

    protected PokedexGUIMixin(Component component) {
        super(component);
    }

    @Shadow public abstract boolean canSelectTab(int tabIndex);
    @Shadow public abstract void displaytabInfoElement(int tabIndex, boolean update);
    @Shadow public abstract void updateTabInfoElement();

    @Inject(method = "setUpTabs", at = @At("TAIL"), remap = false)
    private void cpn$appendHabitatsTab(CallbackInfo ci) {
        var tabButtons = ((PokedexGUIAccessor) this).getTabButtons();
        cpn$habitatsTabIndex = tabButtons.size();
        int tabIndex = cpn$habitatsTabIndex;
        int x = (width - BASE_WIDTH) / 2;
        int y = (height - BASE_HEIGHT) / 2;

        for (int index = 0; index < tabButtons.size(); index++) {
            float buttonX = x + 190.5F - cpn$tabRowShift + index * cpn$tabSpacing;
            var tab = tabButtons.get(index);
            tab.setButtonX(buttonX);
            tab.setX((int) buttonX);
        }

        ScaledButton habitatsButton = new ScaledButton(x + 190.5F - cpn$tabRowShift + tabIndex * cpn$tabSpacing, y + 181.5F, TAB_ICON_SIZE, TAB_ICON_SIZE, cpn$habitatsTabIcon, 0.5F, false, button -> { if (canSelectTab(tabIndex)) displaytabInfoElement(tabIndex, true); });
        tabButtons.add(habitatsButton);
        addRenderableWidget(habitatsButton);
    }

    @ModifyConstant(method = "render", constant = @Constant(floatValue = 191.5F))
    private float cpn$centerTabSelectionArrow(float original) {
        return original - cpn$tabRowShift;
    }

    @ModifyConstant(method = "render", constant = @Constant(floatValue = 22.0F))
    private float cpn$tightenTabSelectionArrowSpacing(float original) {
        return cpn$tabSpacing;
    }

    @Inject(method = "displaytabInfoElement", at = @At("TAIL"), remap = false)
    private void cpn$displayHabitats(int tabIndex, boolean update, CallbackInfo ci) {
        if (tabIndex != cpn$habitatsTabIndex || tabInfoIndex != cpn$habitatsTabIndex) return;
        if (tabInfoElement != null) removeWidget(tabInfoElement);

        int x = (width - BASE_WIDTH) / 2;
        int y = (height - BASE_HEIGHT) / 2;
        tabInfoElement = new HabitatsWidget(x + 189, y + 135);
        if (tabInfoElement instanceof Renderable && tabInfoElement instanceof NarratableEntry) {
            addRenderableWidget(cpn$asRenderable(tabInfoElement));
        }

        var tabButtons = ((PokedexGUIAccessor) this).getTabButtons();
        for (var tab : tabButtons) removeWidget(tab);
        for (var tab : tabButtons) addRenderableWidget(tab);

        if (update) updateTabInfoElement();
    }

    @Inject(method = "updateTabInfoElement", at = @At("TAIL"), remap = false)
    private void cpn$loadHabitats(CallbackInfo ci) {
        if (tabInfoIndex != cpn$habitatsTabIndex || !(tabInfoElement instanceof HabitatsWidget widget)) return;

        PokedexGUIAccessor accessor = (PokedexGUIAccessor) this;
        var selected = accessor.getSelectedEntry();
        var form = accessor.getSelectedForm();
        if (selected == null || form == null) return;

        var entry = ClientNetworkStorage.INSTANCE.getInfo(selected.getSpeciesId());
        if (entry == null) return;
        ClientNetworkEntry clientNetworkEntry = entry.get(form.getDisplayForm());
        if (clientNetworkEntry == null) return;

        int total = clientNetworkEntry.biomes().values().stream().mapToInt(Integer::intValue).sum();
        widget.data = total == 0 ? Map.of() : clientNetworkEntry.biomes().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, biome -> biome.getValue() * 100f / total));
        widget.setEntries();
    }

    @SuppressWarnings("unchecked")
    @Unique
    private static <T extends GuiEventListener & Renderable> T cpn$asRenderable(GuiEventListener listener) {
        return (T) listener;
    }
}
