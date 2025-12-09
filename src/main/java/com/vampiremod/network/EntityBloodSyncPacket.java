package com.vampiremod.network;

import com.vampiremod.capability.ModCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record EntityBloodSyncPacket(int entityId, int current, int max, boolean drinkable) {
    public static void encode(EntityBloodSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.current);
        buf.writeVarInt(msg.max);
        buf.writeBoolean(msg.drinkable);
    }

    public static EntityBloodSyncPacket decode(FriendlyByteBuf buf) {
        return new EntityBloodSyncPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(EntityBloodSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Entity entity = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(msg.entityId()) : null;
                    if (entity != null) {
                        entity.getCapability(ModCapabilities.BLOOD_CAP).ifPresent(cap -> {
                            cap.setMax(msg.max());
                            cap.setCurrent(msg.current());
                            cap.setDrinkable(msg.drinkable());
                        });
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
