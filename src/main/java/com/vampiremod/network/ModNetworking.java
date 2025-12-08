package com.vampiremod.network;

import com.vampiremod.VampireMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(VampireMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        CHANNEL.registerMessage(nextId(), OfferVampireBitePacket.class,
                OfferVampireBitePacket::encode, OfferVampireBitePacket::decode, OfferVampireBitePacket::handle);
        CHANNEL.registerMessage(nextId(), RespondVampireBitePacket.class,
                RespondVampireBitePacket::encode, RespondVampireBitePacket::decode, RespondVampireBitePacket::handle);
        CHANNEL.registerMessage(nextId(), VampireDataSyncPacket.class,
                VampireDataSyncPacket::encode, VampireDataSyncPacket::decode, VampireDataSyncPacket::handle);
    }
}
