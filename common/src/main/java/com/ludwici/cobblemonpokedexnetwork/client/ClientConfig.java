package com.ludwici.cobblemonpokedexnetwork.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Supplier;

public final class ClientConfig {
    public static final String DEFAULT_DATE_FORMAT = "dd.MM.yyyy HH:mm:ss";
    private static final Logger LOGGER = LoggerFactory.getLogger("cobblemon-pokedex-network-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Platform.getConfigFolder().resolve("cobblemon-pokedex-network.json");

    private static Data data = new Data();
    private static Supplier<String> dateFormatSupplier = () -> data.dateFormat;
    private static boolean externalProvider;

    private ClientConfig() {}

    public static synchronized void installDateFormatSupplier(Supplier<String> supplier) {
        dateFormatSupplier = Objects.requireNonNull(supplier, "supplier");
        externalProvider = true;
    }

    public static synchronized void load() {
        if (externalProvider) return;

        try {
            Files.createDirectories(PATH.getParent());
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH)) {
                    Data loaded = GSON.fromJson(reader, Data.class);
                    if (loaded != null) data = loaded;
                }
            }
            data.dateFormat = validated(data.dateFormat);
            save();
        } catch (Exception e) {
            LOGGER.error("Failed to load client config {}, using defaults", PATH, e);
            data = new Data();
        }
    }

    public static String dateFormat() {
        try {
            return validated(dateFormatSupplier.get());
        } catch (Exception e) {
            LOGGER.warn("Failed to read date format, falling back to '{}'", DEFAULT_DATE_FORMAT, e);
            return DEFAULT_DATE_FORMAT;
        }
    }

    public static synchronized void setDateFormat(String value) {
        if (externalProvider) return;
        data.dateFormat = validated(value);
    }

    public static synchronized void save() {
        if (externalProvider) return;

        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save client config {}", PATH, e);
        }
    }

    private static String validated(String pattern) {
        if (pattern == null) return DEFAULT_DATE_FORMAT;
        try {
            DateTimeFormatter.ofPattern(pattern);
            return pattern;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid date format '{}', falling back to '{}'", pattern, DEFAULT_DATE_FORMAT);
            return DEFAULT_DATE_FORMAT;
        }
    }

    private static final class Data {
        String dateFormat = DEFAULT_DATE_FORMAT;
    }
}
