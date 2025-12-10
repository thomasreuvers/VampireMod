package com.vampiremod.capability;

import com.vampiremod.ability.network.AbilitySyncPacket;
import com.vampiremod.ability.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps the ability capability in sync across player lifecycle events and ensures
 * newly added abilities are always present on existing saves.
 */
@Mod.EventBusSubscriber(modid = com.vampiremod.VampireMod.MOD_ID)
public class AbilityCapabilityEvents {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                .ifPresent(oldCap -> event.getEntity().getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                        .ifPresent(newCap -> newCap.deserializeNBT(oldCap.serializeNBT())));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer watcher)) {
            return;
        }

        target.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                .ifPresent(cap -> {
                    cap.ensureAbilitiesRegistered();
                    NetworkHandler.sendToPlayer(
                            new AbilitySyncPacket(target.getId(), cap.serializeNBT()), watcher);
                });
    }

    private static void sync(ServerPlayer player) {
        player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                .ifPresent(cap -> {
                    // Backfill any new abilities before syncing to avoid missing client entries
                    cap.ensureAbilitiesRegistered();
                    if (cap.isBatForm()) {
                        player.refreshDimensions();
                    }
                    NetworkHandler.sendToPlayer(new AbilitySyncPacket(player.getId(), cap.serializeNBT()), player);
                });
    }
}
