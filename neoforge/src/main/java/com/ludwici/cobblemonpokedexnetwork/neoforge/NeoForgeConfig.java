package com.ludwici.cobblemonpokedexnetwork.neoforge;

import com.ludwici.cobblemonpokedexnetwork.client.ClientConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.time.format.DateTimeFormatter;

public final class NeoForgeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> DATE_FORMAT = BUILDER.comment("Date format pattern. See the in-game config screen for available symbols.").translation("config.cobblemonpokedexnetwork.date_format").define("dateFormat", ClientConfig.DEFAULT_DATE_FORMAT, NeoForgeConfig::isValidDateFormat);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private NeoForgeConfig() {}

    private static boolean isValidDateFormat(Object value) {
        if (!(value instanceof String pattern)) return false;
        try {
            DateTimeFormatter.ofPattern(pattern);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
