package com.vampiremod.event;

import com.vampiremod.Capability.BloodData;
import com.vampiremod.Capability.ModCapabilities;
import com.vampiremod.Capability.PlayerVampireData;
import com.vampiremod.network.EntityBloodSyncPacket;
import com.vampiremod.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber
public class VampireFeedHandler {
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        if (event.getLevel().isClientSide) return;
        if (!event.getEntity().isCrouching()) return;

        // Must be a vampire
        if (!event.getEntity().getCapability(ModCapabilities.VAMPIRE_CAP).map(PlayerVampireData::isVampire).orElse(false)) {
            return;
        }

        BloodData blood = target.getCapability(ModCapabilities.BLOOD_CAP).orElse(null);
        if (!blood.isDrinkable() || blood.getCurrent() <= 0) {
            return;
        }

        // Keep blood in sync with current health
        int syncedCurrent = Math.min(blood.getCurrent(), (int) Math.ceil(target.getHealth()));
        blood.setCurrent(syncedCurrent);

        int drained = Math.min(2, blood.getCurrent()); // one drop = 2 blood/health
        if (drained <= 0) return;

        blood.setCurrent(Math.max(0, blood.getCurrent() - drained));
        event.getEntity().getCapability(ModCapabilities.VAMPIRE_CAP).ifPresent(cap -> cap.setBlood(cap.getBlood() + drained));

        // Damage target with the same amount; last drop kills
        target.hurt(target.damageSources().playerAttack(event.getEntity()), drained);
        if (blood.getCurrent() <= 0 || target.getHealth() <= 0) {
            target.hurt(target.damageSources().playerAttack(event.getEntity()), Float.MAX_VALUE);
            blood.setCurrent(0);
        } else {
            // clamp blood to current health
            blood.setCurrent(Math.min(blood.getCurrent(), (int) Math.ceil(target.getHealth())));
        }

        // Play drinking sound for the player
        if (event.getEntity() instanceof ServerPlayer sp) {
            sp.level().playSound(null, sp.blockPosition(), com.vampiremod.Sound.ModSounds.VAMPIRE_DRINK.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        // Sync to player HUD and to tracking clients for entity blood
        ModCapabilities.sync(event.getEntity());
        ModNetworking.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                new EntityBloodSyncPacket(target.getId(), blood.getCurrent(), blood.getMax(), blood.isDrinkable()));

        event.setCanceled(true);
    }
}
