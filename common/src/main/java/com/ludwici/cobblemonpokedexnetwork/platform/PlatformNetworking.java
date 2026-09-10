package com.ludwici.cobblemonpokedexnetwork.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class PlatformNetworking {
    private static BiConsumer<ServerPlayer, CustomPacketPayload> sender;

    private PlatformNetworking() {}

    public static void installSender(BiConsumer<ServerPlayer, CustomPacketPayload> implementation) {
        sender = Objects.requireNonNull(implementation, "implementation");
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (sender == null) {
            throw new IllegalStateException("Platform packet sender was not installed before common initialization");
        }
        sender.accept(player, payload);
    }
}
