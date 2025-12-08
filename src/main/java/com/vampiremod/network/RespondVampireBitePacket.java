package com.vampiremod.network;

import com.vampiremod.Capability.ModCapabilities;
import com.vampiremod.Capability.PlayerVampireData;
import com.vampiremod.Effect.ModEffects;
import com.vampiremod.Entity.VampireEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RespondVampireBitePacket(int vampireEntityId, boolean accept) {
    public static void encode(RespondVampireBitePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.vampireEntityId);
        buf.writeBoolean(msg.accept);
    }

    public static RespondVampireBitePacket decode(FriendlyByteBuf buf) {
        return new RespondVampireBitePacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(RespondVampireBitePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ctx.get().enqueueWork(() -> {
                if (msg.accept) {
                    if (ModEffects.VAMPIRISM_BITE.isPresent()) {
                        player.addEffect(new MobEffectInstance(ModEffects.VAMPIRISM_BITE.get(), 20 * 30, 0, false, true, true));
                    }
                    player.displayClientMessage(Component.literal("You accept the curse."), true);
                } else {
                    player.displayClientMessage(Component.literal("You refuse the curse."), true);
                }

                if (player.level() != null) {
                    var entity = player.level().getEntity(msg.vampireEntityId);
                    if (entity instanceof VampireEntity vampire) {
                        vampire.disperseIntoBats();
                    }
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
