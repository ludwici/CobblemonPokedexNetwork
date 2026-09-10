package com.ludwici.cobblemonpokedexnetwork.fabric.compat;

import com.ludwici.cobblemonpokedexnetwork.client.ClientConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ClothConfigScreen {
    private static final String[] TOOLTIP_SYMBOL_KEYS = {"yyyy", "MM", "dd", "HH", "hh", "mm", "ss", "SSS", "EEEE", "MMMM", "a"};

    private ClothConfigScreen() {}

    public static Screen build(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.translatable("cobblemon-pokedex-network.config.title"));

        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("cobblemon-pokedex-network.config.client_category"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        category.addEntry(entryBuilder.startStrField(Component.translatable("config.cobblemon-pokedex-network.date_format.title"), ClientConfig.dateFormat()).setDefaultValue(ClientConfig.DEFAULT_DATE_FORMAT).setTooltipSupplier(ClothConfigScreen::tooltip).setErrorSupplier(ClothConfigScreen::validationError).setSaveConsumer(ClientConfig::setDateFormat).build());

        builder.setSavingRunnable(ClientConfig::save);
        return builder.build();
    }

    private static Optional<Component[]> tooltip(String pattern) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("config.cobblemon-pokedex-network.date_format.tooltip.header").withStyle(ChatFormatting.YELLOW));

        for (String key : TOOLTIP_SYMBOL_KEYS) {
            lines.add(Component.translatable("config.cobblemon-pokedex-network.date_format.tooltip." + key).withStyle(ChatFormatting.GRAY));
        }

        lines.add(Component.literal(""));
        try {
            String example = LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
            lines.add(Component.translatable("config.cobblemon-pokedex-network.date_format.tooltip.example", example).withStyle(ChatFormatting.GREEN));
        } catch (IllegalArgumentException e) {
            lines.add(Component.translatable("config.cobblemon-pokedex-network.date_format.tooltip.error", e.getMessage()).withStyle(ChatFormatting.RED));
        }

        return Optional.of(lines.toArray(Component[]::new));
    }

    private static Optional<Component> validationError(String pattern) {
        try {
            DateTimeFormatter.ofPattern(pattern);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of(Component.translatable("config.cobblemon-pokedex-network.date_format.tooltip.error", e.getMessage()));
        }
    }
}
