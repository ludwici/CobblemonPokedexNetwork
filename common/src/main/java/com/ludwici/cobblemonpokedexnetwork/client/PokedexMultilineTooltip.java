package com.ludwici.cobblemonpokedexnetwork.client;

import com.cobblemon.mod.common.client.CobblemonResources;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import static com.cobblemon.mod.common.api.gui.GuiUtilsKt.blitk;
import static com.cobblemon.mod.common.client.render.RenderHelperKt.drawScaledText;
import static com.cobblemon.mod.common.util.MiscUtilsKt.cobblemonResource;

public final class PokedexMultilineTooltip {
    private static final ResourceLocation TOOLTIP_EDGE = cobblemonResource("textures/gui/pokedex/tooltip_edge.png");
    private static final ResourceLocation TOOLTIP_BACKGROUND = cobblemonResource("textures/gui/pokedex/tooltip_background.png");

    private PokedexMultilineTooltip() {}

    public static void render(GuiGraphics context, MutableComponent text, int posX, int posY, float delta, int offsetY) {
        PoseStack poseStack = context.pose();
        Font font = Minecraft.getInstance().font;
        String[] lines = text.getString().split("\\\\n");
        int lineHeight = 10;
        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, font.width(Component.literal(line).withStyle(ChatFormatting.BOLD)));
        }
        int tooltipWidth = textWidth + 2;
        int tooltipHeight = 11 + (lines.length - 1) * lineHeight;
        int tooltipTop = posY + offsetY;

        poseStack.pushPose();
        poseStack.translate(0.0, 0.0, 1000.0);
        Minecraft mc = Minecraft.getInstance();
        mc.getMainRenderTarget().bindWrite(false);
        context.enableScissor(posX - tooltipWidth / 2, tooltipTop + 1, posX - tooltipWidth / 2 + tooltipWidth, tooltipTop + tooltipHeight - 1);
        mc.gameRenderer.processBlurEffect(delta);
        context.disableScissor();
        mc.getMainRenderTarget().bindWrite(true);

        blitk(poseStack, TOOLTIP_EDGE, posX - tooltipWidth / 2 - 1, tooltipTop, tooltipHeight, 1);
        blitk(poseStack, TOOLTIP_BACKGROUND, posX - tooltipWidth / 2, tooltipTop, tooltipHeight, tooltipWidth);
        blitk(poseStack, TOOLTIP_EDGE, posX + tooltipWidth / 2, tooltipTop, tooltipHeight, 1);
        for (int i = 0; i < lines.length; i++) {
            drawScaledText(context, CobblemonResources.INSTANCE.getDEFAULT_LARGE(), Component.literal(lines[i]).withStyle(ChatFormatting.BOLD), posX, tooltipTop + 1 + i * lineHeight, 1F, 1F, Integer.MAX_VALUE, 0xFFFFFFFF, true, true, null, null);
        }
        poseStack.popPose();
    }
}
