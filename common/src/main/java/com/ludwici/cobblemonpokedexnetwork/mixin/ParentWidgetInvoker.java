package com.ludwici.cobblemonpokedexnetwork.mixin;

import com.cobblemon.mod.common.api.gui.ParentWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ParentWidget.class)
public interface ParentWidgetInvoker {
    @Invoker("addWidget")
    void invokeAddWidget(GuiEventListener widget);
}
