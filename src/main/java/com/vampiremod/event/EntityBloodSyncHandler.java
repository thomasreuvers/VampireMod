package com.vampiremod.event;

import com.vampiremod.capability.ModCapabilities;
import com.vampiremod.network.EntityBloodSyncPacket;
import com.vampiremod.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber
public class EntityBloodSyncHandler {
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity)) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        target.getCapability(ModCapabilities.BLOOD_CAP).ifPresent(cap -> {
            ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new EntityBloodSyncPacket(target.getId(), cap.getCurrent(), cap.getMax(), cap.isDrinkable()));
        });
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        var level = sp.serverLevel();
        double range = 64.0D;
        var nearby = level.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(range));
        for (LivingEntity living : nearby) {
            living.getCapability(ModCapabilities.BLOOD_CAP).ifPresent(cap -> ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new EntityBloodSyncPacket(living.getId(), cap.getCurrent(), cap.getMax(), cap.isDrinkable())));
        }
    }
}
