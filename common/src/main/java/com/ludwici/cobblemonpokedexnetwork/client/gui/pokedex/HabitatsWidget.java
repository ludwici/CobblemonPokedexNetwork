package com.ludwici.cobblemonpokedexnetwork.client.gui.pokedex;

import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.gui.ScrollingWidget;
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants;
import com.cobblemon.mod.common.client.render.RenderHelperKt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class HabitatsWidget extends ScrollingWidget<HabitatsWidget.HabitatWidgetEntry> {
    private final int pX;
    private final int pY;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    public Map<ResourceLocation, Float> data = new HashMap<>();

    public HabitatsWidget(int pX, int pY) {
        super(pY + 10, pX, PokedexGUIConstants.HALF_OVERLAY_WIDTH - 2, 42, 10, 5);
        this.pX = pX;
        this.pY = pY;
    }

    public void setEntries() {
        data.forEach((key, value) -> addEntry(new HabitatWidgetEntry(key, value)));
    }

    @Override public int getX() { return pX; }
    @Override public int getY() { return pY; }
    @Override protected int getScrollbarPosition() { return getLeft() + getWidth() + getScrollBarWidth() - 7; }
    @Override public int getBottom() { return getY() + height - 1; }

    @Override
    public void renderScrollbar(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int xLeft = getScrollbarPosition();
        int xRight = xLeft + 3;
        int yStart = getY() + 1;
        int barHeight = getBottom() - yStart;
        int yBottom = (int) ((float) (barHeight * barHeight) / (float) getMaxPosition());
        yBottom = Mth.clamp(yBottom, 32, barHeight - 8);
        int yTop = ((int) getScrollAmount()) * (barHeight - yBottom) / getMaxScroll() + yStart;
        if (yTop < yStart) yTop = yStart;
        context.fill(xLeft + 1, yStart, xRight - 1, getBottom(), FastColor.ARGB32.color(255, 126, 231, 229));
        context.fill(xLeft, yTop, xRight, yTop + yBottom, FastColor.ARGB32.color(255, 58, 150, 182));
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        RenderHelperKt.drawScaledText(context, CobblemonResources.INSTANCE.getDEFAULT_LARGE(), Component.translatable("cobblemon-pokedex-network.gui.habitats").withStyle(ChatFormatting.BOLD), pX, pY - 10, 1, 1, Integer.MAX_VALUE, 0x00FFFFFF, false, true, null, null);
        if (data.isEmpty()) {
            RenderHelperKt.drawScaledText(context, Component.translatable("cobblemon-pokedex-network.gui.no_data").getVisualOrderText(), pX + width / 2 - 7, pY + height / 2 - 3, PokedexGUIConstants.SCALE, 1, Integer.MAX_VALUE, 0x606B6E, true, false);
        } else {
            super.renderWidget(context, mouseX, mouseY, delta);
        }
    }

    @Override
    protected void renderItem(GuiGraphics context, int mouseX, int mouseY, float delta, int index, int x, int y, int entryWidth, int entryHeight) {
        var entry = getEntry(index);
        entry.render(context, index, x, y + 2, entryWidth, entryHeight, mouseX, mouseY, getHovered() == entry, delta);
    }

    @Override
    protected HabitatWidgetEntry getEntry(int i) {
        return children().get(i);
    }

    public class HabitatWidgetEntry extends Slot<HabitatWidgetEntry> {
        private final ResourceLocation biome;
        private final float percentage;

        public HabitatWidgetEntry(ResourceLocation biome, float percentage) {
            this.biome = biome;
            this.percentage = percentage;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("biome." + biome.toLanguageKey());
        }

        @Override
        public void render(GuiGraphics context, int index, int x, int y, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.pose().pushPose();
            context.pose().translate(0f, 0f, 100f);
            MutableComponent displayText = Component.translatable("biome." + biome.toLanguageKey()).withStyle(ChatFormatting.BOLD);
            RenderHelperKt.drawScaledText(context, displayText.getVisualOrderText(), x, y + 2, PokedexGUIConstants.SCALE, PokedexGUIConstants.SCALE, 1, 0x606B6E, false, false);
            RenderHelperKt.drawScaledTextJustifiedRight(context, Component.literal(DECIMAL_FORMAT.format(percentage) + "%"), x + 120, y + 3, PokedexGUIConstants.SCALE, PokedexGUIConstants.SCALE, 1, 0x606B6E, false);
            context.pose().popPose();
        }
    }
}
