package com.vampiremod.network;

import com.vampiremod.Capability.ModCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record VampireDataSyncPacket(boolean vampire, float blood) {
    public static void encode(VampireDataSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.vampire);
        buf.writeFloat(msg.blood);
    }

    public static VampireDataSyncPacket decode(FriendlyByteBuf buf) {
        return new VampireDataSyncPacket(buf.readBoolean(), buf.readFloat());
    }

    public static void handle(VampireDataSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.getCapability(ModCapabilities.VAMPIRE_CAP).ifPresent(cap -> {
                            cap.setVampire(msg.vampire());
                            cap.setBlood(msg.blood());
                        });
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
