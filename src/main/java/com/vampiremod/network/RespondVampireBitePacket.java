package com.vampiremod.network;

import com.vampiremod.Capability.PlayerVampireData;
import com.vampiremod.Capability.ModCapabilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RespondVampireBitePacket(boolean accept) {
    public static void encode(RespondVampireBitePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.accept);
    }

    public static RespondVampireBitePacket decode(FriendlyByteBuf buf) {
        return new RespondVampireBitePacket(buf.readBoolean());
    }

    public static void handle(RespondVampireBitePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ctx.get().enqueueWork(() -> {
                if (msg.accept) {
                    player.getCapability(ModCapabilities.VAMPIRE_CAP).ifPresent(cap -> cap.setVampire(true));
                } else {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("You refuse the curse."), true);
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
