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
    private final int entityId;
    private final CompoundTag data;

    public AbilitySyncPacket(int entityId, CompoundTag data) {
        this.entityId = entityId;
        this.data = data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeNbt(data);
    }

    public static AbilitySyncPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        return new AbilitySyncPacket(id, buf.readNbt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            Player player = null;
            var entity = mc.level.getEntity(entityId);
            if (entity instanceof Player p) {
                player = p;
            } else if (mc.player != null && mc.player.getId() == entityId) {
                player = mc.player;
            }

            if (player != null && data != null) {
                player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                        .ifPresent(cap -> cap.deserializeNBT(data));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
