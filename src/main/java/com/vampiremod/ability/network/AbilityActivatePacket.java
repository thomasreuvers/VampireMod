package com.vampiremod.ability.network;

import com.vampiremod.ability.AbilityInstance;
import com.vampiremod.capability.AbilityCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AbilityActivatePacket {
    private final ResourceLocation abilityId;

    public AbilityActivatePacket(ResourceLocation abilityId) {
        this.abilityId = abilityId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(abilityId);
    }

    public static AbilityActivatePacket decode(FriendlyByteBuf buf) {
        return new AbilityActivatePacket(buf.readResourceLocation());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                        .ifPresent(cap -> {
                            AbilityInstance instance = cap.getAbility(abilityId);
                            if (instance != null && instance.isUnlocked()) {
                                instance.activate(player);
                            }
                        });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
