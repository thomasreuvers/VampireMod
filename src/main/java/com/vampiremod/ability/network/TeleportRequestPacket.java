package com.vampiremod.ability.network;

import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.ability.impl.TeleportAbility;
import com.vampiremod.capability.AbilityCapabilityProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TeleportRequestPacket {
    private final BlockPos targetPos;

    public TeleportRequestPacket(BlockPos targetPos) {
        this.targetPos = targetPos;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(targetPos);
    }

    public static TeleportRequestPacket decode(FriendlyByteBuf buf) {
        return new TeleportRequestPacket(buf.readBlockPos());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY).ifPresent(cap -> {
                if (!TeleportAbility.ID.equals(cap.getActiveAbility())) {
                    return;
                }

                AbilityInstance instance = cap.getAbility(TeleportAbility.ID);
                if (instance == null || !instance.isUnlocked()) {
                    return;
                }
                if (!(instance.getAbility() instanceof TeleportAbility ability)) {
                    return;
                }

                ability.setPendingTarget(instance, targetPos);
                if (instance.activate(player)) {
                    NetworkHandler.sendToTrackingAndSelf(player,
                            new AbilitySyncPacket(player.getId(), cap.serializeNBT()));
                } else {
                    ability.clearPendingTarget(instance);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
