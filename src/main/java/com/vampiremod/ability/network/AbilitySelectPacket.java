package com.vampiremod.ability.network;

import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.capability.AbilityCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Selects the active ability without activating it.
 */
public class AbilitySelectPacket {
    private final ResourceLocation abilityId;

    public AbilitySelectPacket(ResourceLocation abilityId) {
        this.abilityId = abilityId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(abilityId);
    }

    public static AbilitySelectPacket decode(FriendlyByteBuf buf) {
        return new AbilitySelectPacket(buf.readResourceLocation());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY).ifPresent(cap -> {
                AbilityInstance instance = cap.getAbility(abilityId);
                if (instance != null && instance.isUnlocked()) {
                    cap.setActiveAbility(abilityId);
                    NetworkHandler.sendToTrackingAndSelf(player,
                            new AbilitySyncPacket(player.getId(), cap.serializeNBT()));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
