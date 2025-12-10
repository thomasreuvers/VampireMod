package com.vampiremod.event;

import com.vampiremod.VampireMod;
import com.vampiremod.capability.AbilityCapabilityProvider;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/**
 * Replaces player sounds with bat variants while in bat form.
 */
@Mod.EventBusSubscriber(modid = VampireMod.MOD_ID)
public class BatSoundHandler {
    private BatSoundHandler() {
    }

    @SubscribeEvent
    public static void onPlaySound(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isBat(player)) {
            return;
        }

        Holder<SoundEvent> replacement = mapToBatSound(event.getSound());
        if (replacement != null) {
            event.setSound(replacement);
        }
    }

    private static Holder<SoundEvent> mapToBatSound(Holder<SoundEvent> original) {
        if (original == null || !original.isBound()) {
            return null;
        }

        ResourceLocation id = original.value().getLocation();
        String path = id.getPath().toLowerCase(Locale.ROOT);

        if (path.contains("step")) {
            return Holder.direct(SoundEvents.BAT_TAKEOFF);
        }
        if (path.contains("hurt") || path.contains("damage")) {
            return Holder.direct(SoundEvents.BAT_HURT);
        }
        if (path.contains("death")) {
            return Holder.direct(SoundEvents.BAT_DEATH);
        }
        if (path.contains("ambient") || path.contains("idle")) {
            return Holder.direct(SoundEvents.BAT_AMBIENT);
        }

        return null;
    }

    private static boolean isBat(Player player) {
        return player.getCapability(AbilityCapabilityProvider.ABILITY_CAPABILITY)
                .map(cap -> cap.isBatForm())
                .orElse(false);
    }
}
