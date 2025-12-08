package com.vampiremod.Capability;

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

public class ModCapabilities {

    public static final Capability<PlayerVampireData> VAMPIRE_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerVampireData.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(PlayerVampireProvider.ID, new PlayerVampireProvider());
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
}
