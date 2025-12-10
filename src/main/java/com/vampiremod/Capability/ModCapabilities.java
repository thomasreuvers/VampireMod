package com.vampiremod.capability;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * Central capability wiring: registers all capabilities on the MOD bus,
 * attaches them on the Forge bus, and keeps core vampire data in sync.
 */
public class ModCapabilities {

    public static final Capability<PlayerVampireData> VAMPIRE_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<BloodData> BLOOD_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IAbilityCapability> ABILITY_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerVampireData.class);
        event.register(BloodData.class);
        event.register(IAbilityCapability.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(PlayerVampireProvider.ID, new PlayerVampireProvider());
            event.addCapability(AbilityCapabilityProvider.ID, new AbilityCapabilityProvider());
        }
        if (event.getObject() instanceof net.minecraft.world.entity.LivingEntity living) {
            event.addCapability(BloodProvider.ID, new BloodProvider());
        }
    }

    @SubscribeEvent
    public static void clonePlayerData(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();

        LazyOptional<PlayerVampireData> original = event.getOriginal().getCapability(VAMPIRE_CAP);
        LazyOptional<PlayerVampireData> copy = event.getEntity().getCapability(VAMPIRE_CAP);

        copy.ifPresent(newCap -> original.ifPresent(newCap::copyFrom));

        event.getOriginal().invalidateCaps();
    }

    public static void sync(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            player.getCapability(VAMPIRE_CAP).ifPresent(cap -> {
                var packet = new com.vampiremod.network.VampireDataSyncPacket(cap.isVampire(), cap.getBlood());
                com.vampiremod.network.ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
            });
        }
    }
}
