package com.vampiremod.ability.network;

import com.vampiremod.capability.AbilityCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to sync ability data
 */
public class AbilitySyncPacket {
    private final CompoundTag data;

    public AbilitySyncPacket(CompoundTag data) {
        this.data = data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public static AbilitySyncPacket decode(FriendlyByteBuf buf) {
        return new AbilitySyncPacket(buf.readNbt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                        .ifPresent(cap -> cap.deserializeNBT(data));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
