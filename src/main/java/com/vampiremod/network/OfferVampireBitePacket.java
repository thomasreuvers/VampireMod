package com.vampiremod.network;

import com.vampiremod.client.VampireOfferScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OfferVampireBitePacket(int vampireEntityId) {
    public static void encode(OfferVampireBitePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.vampireEntityId);
    }

    public static OfferVampireBitePacket decode(FriendlyByteBuf buf) {
        return new OfferVampireBitePacket(buf.readVarInt());
    }

    public static void handle(OfferVampireBitePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.setScreen(new VampireOfferScreen(msg.vampireEntityId));
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
