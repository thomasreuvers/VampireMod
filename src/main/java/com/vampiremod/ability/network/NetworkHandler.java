package com.vampiremod.ability.network;

import com.vampiremod.VampireMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    public static void register() {
        INSTANCE = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(VampireMod.MOD_ID, "abilities"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE.messageBuilder(AbilityActivatePacket.class, packetId++)
                .encoder(AbilityActivatePacket::encode)
                .decoder(AbilityActivatePacket::decode)
                .consumerMainThread(AbilityActivatePacket::handle)
                .add();

        INSTANCE.messageBuilder(AbilitySyncPacket.class, packetId++)
                .encoder(AbilitySyncPacket::encode)
                .decoder(AbilitySyncPacket::decode)
                .consumerMainThread(AbilitySyncPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
